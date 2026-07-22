package com.myreport.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.config.SimpleReportProperties;
import com.myreport.entity.SimpleReport;
import com.myreport.entity.SimpleReportBlock;
import com.myreport.entity.SimpleReportRun;
import com.myreport.framework.redis.RedisTemplate;
import com.myreport.repository.SimpleReportBlockRepository;
import com.myreport.repository.SimpleReportRepository;
import com.myreport.repository.SimpleReportRunRepository;
import com.myreport.util.Constant;
import com.myreport.util.FileUtil;
import com.myreport.util.word.SimpleReportUtil;
import com.myreport.util.word.common.CommonUtil;
import com.myreport.vo.CreateReportVO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 简化报告：配置 + plan/confirm + 生成编排。
 *
 * @see docs/ai_simple_report/API设计.md
 * @see docs/simple_report_status/API设计.md
 */
@Service
public class SimpleReportService {

    private static final Logger logger = Logger.getLogger(SimpleReportService.class);

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final int LOCK_TTL_SECONDS = 3600;
    private static final int MAX_BLOCKS = 50;
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SimpleReportRepository reportRepository;
    private final SimpleReportBlockRepository blockRepository;
    private final SimpleReportRunRepository runRepository;
    private final SimpleReportHttpFetcher httpFetcher;
    private final SimpleReportAssembleService assembleService;
    private final SimpleReportProperties properties;
    private final SimpleReportMailService mailService;

    public SimpleReportService(SimpleReportRepository reportRepository,
                               SimpleReportBlockRepository blockRepository,
                               SimpleReportRunRepository runRepository,
                               SimpleReportHttpFetcher httpFetcher,
                               SimpleReportAssembleService assembleService,
                               SimpleReportProperties properties,
                               SimpleReportMailService mailService) {
        this.reportRepository = reportRepository;
        this.blockRepository = blockRepository;
        this.runRepository = runRepository;
        this.httpFetcher = httpFetcher;
        this.assembleService = assembleService;
        this.properties = properties;
        this.mailService = mailService;
    }

    public Page<SimpleReport> list(int page, int size, Integer status) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 200);
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "updateTime"));
        if (status != null) {
            return reportRepository.findByDeletedAndStatus(NOT_DELETED, status, pr);
        }
        return reportRepository.findByDeleted(NOT_DELETED, pr);
    }

    public Map<String, Object> toListItem(SimpleReport r) {
        return toListItem(r, null, false);
    }

    /**
     * @param displayRunHint 展示用 run；可为 null（未生成）
     * @param useProvidedDisplay true 时直接使用 hint（含 null）；false 时自行 resolve
     */
    public Map<String, Object> toListItem(SimpleReport r, SimpleReportRun displayRunHint, boolean useProvidedDisplay) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("status", r.getStatus());
        m.put("deliveryDir", r.getDeliveryDir());
        m.put("notifyEmail", r.getNotifyEmail());
        m.put("scheduleEnabled", r.getScheduleEnabled());
        m.put("cronExpr", r.getCronExpr());
        m.put("blockCount", blockRepository.countByReportId(r.getId()));
        SimpleReportRun latest = findLatestDownloadableRun(r.getId());
        m.put("canDownload", latest != null);
        m.put("latestSuccessRunId", latest != null ? latest.getId() : null);
        SimpleReportRun display = useProvidedDisplay ? displayRunHint : resolveDisplayRun(r.getId());
        putDisplayRunFields(m, display);
        m.put("createTime", r.getCreateTime());
        m.put("updateTime", r.getUpdateTime());
        return m;
    }

    /**
     * 为本页配置批量解析「列表展示用」run，避免 N+1。每个 id 均有键（无 run 时值为 null）。
     */
    public Map<Long, SimpleReportRun> resolveDisplayRuns(Collection<Long> reportIds) {
        Map<Long, SimpleReportRun> out = new HashMap<Long, SimpleReportRun>();
        if (reportIds == null || reportIds.isEmpty()) {
            return out;
        }
        for (Long id : reportIds) {
            if (id != null) {
                out.put(id, null);
            }
        }
        List<SimpleReportRun> all = runRepository.findByReportIdInOrderByCreateTimeDescIdDesc(reportIds);
        if (all == null || all.isEmpty()) {
            return out;
        }
        Map<Long, List<SimpleReportRun>> byReport = new HashMap<Long, List<SimpleReportRun>>();
        for (SimpleReportRun run : all) {
            if (run == null || run.getReportId() == null) {
                continue;
            }
            List<SimpleReportRun> list = byReport.get(run.getReportId());
            if (list == null) {
                list = new ArrayList<SimpleReportRun>();
                byReport.put(run.getReportId(), list);
            }
            list.add(run);
        }
        for (Map.Entry<Long, List<SimpleReportRun>> e : byReport.entrySet()) {
            out.put(e.getKey(), pickDisplayRun(e.getValue()));
        }
        return out;
    }

    public Map<String, Object> detail(Long id) {
        SimpleReport r = requireReport(id);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("status", r.getStatus());
        m.put("deliveryDir", r.getDeliveryDir());
        m.put("notifyEmail", r.getNotifyEmail());
        m.put("scheduleEnabled", r.getScheduleEnabled());
        m.put("cronExpr", r.getCronExpr());
        m.put("createTime", r.getCreateTime());
        m.put("updateTime", r.getUpdateTime());
        List<SimpleReportBlock> blocks = blockRepository.findByReportIdOrderBySortOrderAscIdAsc(id);
        List<Map<String, Object>> blockMaps = new ArrayList<Map<String, Object>>();
        for (SimpleReportBlock b : blocks) {
            blockMaps.add(toBlockMap(b));
        }
        m.put("blocks", blockMaps);
        SimpleReportRun latest = findLatestDownloadableRun(id);
        m.put("canDownload", latest != null);
        m.put("latestSuccessRunId", latest != null ? latest.getId() : null);
        putDisplayRunFields(m, resolveDisplayRun(id));
        return m;
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        String name = requireName(str(body.get("name")));
        if (reportRepository.existsByNameAndDeleted(name, NOT_DELETED)) {
            throw new IllegalArgumentException("报告名称已存在");
        }
        String deliveryDir = requireDeliveryDir(str(body.get("deliveryDir")));
        SimpleReport r = new SimpleReport();
        r.setName(name);
        r.setDeliveryDir(deliveryDir);
        r.setNotifyEmail(normalizeNotifyEmail(str(body.get("notifyEmail"))));
        r.setStatus(toInt(body.get("status"), SimpleReport.STATUS_ENABLED));
        int scheduleEnabled = toInt(body.get("scheduleEnabled"), 0);
        r.setScheduleEnabled(scheduleEnabled);
        String cron = str(body.get("cronExpr"));
        if (scheduleEnabled == 1 && StringUtils.isBlank(cron)) {
            throw new IllegalArgumentException("启用定时时须填写 cronExpr");
        }
        r.setCronExpr(StringUtils.isBlank(cron) ? null : cron.trim());
        r.setDeleted(NOT_DELETED);
        reportRepository.save(r);

        Object blocksObj = body.get("blocks");
        if (blocksObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) blocksObj;
            replaceBlocks(r.getId(), list);
        }
        return r.getId();
    }

    @Transactional
    public void update(Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        SimpleReport r = requireReport(id);
        if (body.containsKey("name")) {
            String name = requireName(str(body.get("name")));
            if (reportRepository.existsByNameAndDeletedAndIdNot(name, NOT_DELETED, id)) {
                throw new IllegalArgumentException("报告名称已存在");
            }
            r.setName(name);
        }
        if (body.containsKey("deliveryDir")) {
            r.setDeliveryDir(requireDeliveryDir(str(body.get("deliveryDir"))));
        }
        if (body.containsKey("notifyEmail")) {
            r.setNotifyEmail(normalizeNotifyEmail(str(body.get("notifyEmail"))));
        }
        if (body.containsKey("status")) {
            r.setStatus(toInt(body.get("status"), r.getStatus()));
        }
        if (body.containsKey("scheduleEnabled") || body.containsKey("cronExpr")) {
            int scheduleEnabled = body.containsKey("scheduleEnabled")
                    ? toInt(body.get("scheduleEnabled"), 0)
                    : (r.getScheduleEnabled() == null ? 0 : r.getScheduleEnabled());
            String cron = body.containsKey("cronExpr") ? str(body.get("cronExpr")) : r.getCronExpr();
            if (scheduleEnabled == 1 && StringUtils.isBlank(cron)) {
                throw new IllegalArgumentException("启用定时时须填写 cronExpr");
            }
            r.setScheduleEnabled(scheduleEnabled);
            r.setCronExpr(StringUtils.isBlank(cron) ? null : cron.trim());
        }
        reportRepository.save(r);
    }

    @Transactional
    public int saveBlocks(Long id, List<Object> blocks) {
        requireReport(id);
        return replaceBlocks(id, blocks == null ? new ArrayList<Object>() : blocks);
    }

    @Transactional
    public void delete(Long id) {
        SimpleReport r = requireReport(id);
        r.setDeleted(DELETED);
        r.setDeleteTime(LocalDateTime.now());
        reportRepository.save(r);
        blockRepository.deleteByReportId(id);
    }

    @Transactional
    public Map<String, Object> plan(Long id, String userPrompt, int triggerType) {
        return plan(id, userPrompt, null, triggerType);
    }

    /**
     * 生成计划。若传入 blocks，先整表替换再出计划（与当前界面配置原子同步）。
     */
    @Transactional
    public Map<String, Object> plan(Long id, String userPrompt, List<Object> blocksToSync, int triggerType) {
        SimpleReport r = requireReport(id);
        if (r.getStatus() == null || r.getStatus() != SimpleReport.STATUS_ENABLED) {
            throw new IllegalArgumentException("报告已停用");
        }
        if (blocksToSync != null) {
            if (blocksToSync.isEmpty()) {
                throw new IllegalArgumentException("请先配置至少一个数据区块");
            }
            replaceBlocks(id, blocksToSync);
        }
        List<SimpleReportBlock> blocks = blockRepository.findByReportIdOrderBySortOrderAscIdAsc(id);
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("请先配置至少一个数据区块");
        }
        for (SimpleReportBlock b : blocks) {
            httpFetcher.validateUrl(b.getUrl());
        }

        String prompt = userPrompt == null ? "" : userPrompt.trim();
        if (prompt.length() > 2000) {
            throw new IllegalArgumentException("userPrompt 过长");
        }

        JSONObject snapshot = buildSnapshot(r, blocks);
        String planMd = buildPlanMd(r, blocks, prompt);

        SimpleReportRun run = new SimpleReportRun();
        run.setReportId(r.getId());
        run.setReportName(r.getName());
        run.setUserPrompt(StringUtils.isBlank(prompt) ? null : prompt);
        run.setPlanMd(planMd);
        run.setConfigSnapshotJson(snapshot.toJSONString());
        run.setRunStatus(SimpleReportRun.STATUS_PENDING_CONFIRM);
        run.setTriggerType(triggerType);
        run.setWarnCount(0);
        runRepository.save(run);

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("runId", run.getId());
        out.put("runStatus", run.getRunStatus());
        out.put("planMd", planMd);
        out.put("userPrompt", run.getUserPrompt() == null ? "" : run.getUserPrompt());
        out.put("blockCount", blocks.size());
        return out;
    }

    @Transactional
    public void updatePlanMd(Long runId, String planMd) {
        SimpleReportRun run = requireRun(runId);
        if (run.getRunStatus() == null
                || run.getRunStatus() != SimpleReportRun.STATUS_PENDING_CONFIRM) {
            throw new IllegalArgumentException("仅待确认状态可修改计划文案");
        }
        if (StringUtils.isBlank(planMd)) {
            throw new IllegalArgumentException("planMd 不能为空");
        }
        run.setPlanMd(planMd);
        runRepository.save(run);
    }

    @Transactional
    public void cancel(Long runId) {
        SimpleReportRun run = requireRun(runId);
        if (run.getRunStatus() == null
                || run.getRunStatus() != SimpleReportRun.STATUS_PENDING_CONFIRM) {
            throw new IllegalArgumentException("仅待确认状态可取消");
        }
        run.setRunStatus(SimpleReportRun.STATUS_CANCELLED);
        run.setFinishedTime(LocalDateTime.now());
        runRepository.save(run);
    }

    /**
     * 确认执行：分配 engineReportId，异步生成。
     */
    public Map<String, Object> confirm(Long runId) {
        SimpleReportRun run = requireRun(runId);
        if (run.getRunStatus() == null
                || run.getRunStatus() != SimpleReportRun.STATUS_PENDING_CONFIRM) {
            throw new IllegalArgumentException("仅待确认状态可执行生成");
        }
        if (run.getId() > Integer.MAX_VALUE - SimpleReportRun.ENGINE_ID_BASE) {
            throw new IllegalArgumentException("运行 ID 超出生成侧限制");
        }
        int engineId = SimpleReportRun.ENGINE_ID_BASE + run.getId().intValue();
        String lockKey = String.format(Constant.RedisKey.REPORT_LOCK, engineId);
        if (!RedisTemplate.setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS)) {
            throw new IllegalArgumentException("报告正在生成中，请稍后再试");
        }

        run.setEngineReportId(engineId);
        run.setRunStatus(SimpleReportRun.STATUS_GENERATING);
        run.setConfirmedTime(LocalDateTime.now());
        run.setFailMessage(null);
        runRepository.save(run);

        try {
            JSONObject snapshot = JSON.parseObject(run.getConfigSnapshotJson());
            SimpleReportAssembleService.AssembleResult assembled = assembleService.assembleFromSnapshot(snapshot);

            run.setWarnCount(assembled.getWarnCount());
            runRepository.save(run);

            JSONObject overallSetting = new JSONObject(true);
            overallSetting.put("strCoverImg", "");
            overallSetting.put("strBackCoverImg", "");
            overallSetting.put("nMetricsCount", Math.max(assembled.getMetricCount(), 1));
            overallSetting.put("simpleReportSync", true);

            CreateReportVO vo = new CreateReportVO();
            vo.setReportId(engineId);
            vo.setReportName(sanitizeFileName(run.getReportName()));
            vo.setReportJsonArr(assembled.getReportJsonArr());
            vo.setOverallSetting(overallSetting);
            vo.set("simpleReportSync", true);

            SimpleReportUtil.createReport(assembled.getReportJsonArr(), overallSetting, vo);
        } catch (Exception e) {
            logger.error("simple-report confirm failed, runId=" + runId, e);
            RedisTemplate.delete(lockKey);
            run.setRunStatus(SimpleReportRun.STATUS_FAILED);
            run.setFailMessage(truncate("提交生成失败：" + e.getMessage(), 512));
            run.setFinishedTime(LocalDateTime.now());
            runRepository.save(run);
            throw new IllegalArgumentException(run.getFailMessage());
        }

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("runId", run.getId());
        out.put("runStatus", SimpleReportRun.STATUS_GENERATING);
        out.put("engineReportId", engineId);
        return out;
    }

    public Map<String, Object> runDetail(Long runId, boolean includeSnapshot) {
        SimpleReportRun run = requireRun(runId);
        Map<String, Object> m = toRunMap(run, true);
        if (includeSnapshot) {
            m.put("configSnapshotJson", run.getConfigSnapshotJson());
        }
        return m;
    }

    public Page<SimpleReportRun> listRuns(Long reportId, Integer runStatus, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 200);
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createTime"));
        if (reportId != null && runStatus != null) {
            return runRepository.findByReportIdAndRunStatus(reportId, runStatus, pr);
        }
        if (reportId != null) {
            return runRepository.findByReportId(reportId, pr);
        }
        if (runStatus != null) {
            return runRepository.findByRunStatus(runStatus, pr);
        }
        return runRepository.findAllByOrderByCreateTimeDesc(pr);
    }

    public Map<String, Object> toRunListItem(SimpleReportRun run) {
        return toRunMap(run, false);
    }

    /**
     * 按 runId 下载：仅 SUCCESS 且文件有效；禁止客户端传路径。
     */
    public DownloadPayload prepareDownloadByRunId(Long runId) {
        SimpleReportRun run = requireRun(runId);
        return prepareDownloadFromRun(run);
    }

    /**
     * 按配置 id 下载最近一次可下载 SUCCESS 稿；配置须未软删。
     */
    public DownloadPayload prepareDownloadByReportId(Long reportId) {
        requireReport(reportId);
        SimpleReportRun run = findLatestDownloadableRun(reportId);
        if (run == null) {
            throw new IllegalArgumentException("暂无可下载文件");
        }
        return prepareDownloadFromRun(run);
    }

    /** @deprecated 请用 {@link #prepareDownloadByRunId(Long)} */
    public File resolveDownloadFile(Long runId) {
        return prepareDownloadByRunId(runId).getFile();
    }

    public static final class DownloadPayload {
        private final File file;
        private final String fileName;

        public DownloadPayload(File file, String fileName) {
            this.file = file;
            this.fileName = fileName;
        }

        public File getFile() {
            return file;
        }

        public String getFileName() {
            return fileName;
        }
    }

    private DownloadPayload prepareDownloadFromRun(SimpleReportRun run) {
        if (run.getRunStatus() == null || run.getRunStatus() != SimpleReportRun.STATUS_SUCCESS) {
            throw new IllegalArgumentException("报告尚未生成成功");
        }
        String path = resolveStoredPath(run);
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("暂无可下载文件");
        }
        File file = new File(path.trim());
        assertUnderAllowedRoot(file, run.getId());
        if (!file.isFile() || !file.exists()) {
            throw new IllegalArgumentException("文件不存在或已被清理");
        }
        String downloadName = sanitizeFileName(run.getReportName());
        if (StringUtils.isBlank(downloadName) || "simple-report".equals(downloadName)) {
            downloadName = "simple-report-" + run.getId();
        }
        if (!downloadName.toLowerCase().endsWith(".docx")) {
            downloadName = downloadName + ".docx";
        }
        return new DownloadPayload(file, downloadName);
    }

    private SimpleReportRun findLatestDownloadableRun(Long reportId) {
        if (reportId == null) {
            return null;
        }
        PageRequest pr = PageRequest.of(0, 20);
        List<SimpleReportRun> runs = runRepository.findByReportIdAndRunStatusOrderByFinishedTimeDescIdDesc(
                reportId, SimpleReportRun.STATUS_SUCCESS, pr);
        if (runs == null || runs.isEmpty()) {
            return null;
        }
        for (SimpleReportRun run : runs) {
            if (canDownload(run)) {
                return run;
            }
        }
        return null;
    }

    /**
     * 列表/详情展示用 run：取该配置下最近一次运行（create_time DESC, id DESC）。
     * 不再优先「待确认」——否则历史未确认计划会盖住更新的成功态。
     *
     * @see docs/simple_report_status/API设计.md
     */
    private SimpleReportRun resolveDisplayRun(Long reportId) {
        if (reportId == null) {
            return null;
        }
        PageRequest one = PageRequest.of(0, 1);
        List<SimpleReportRun> any = runRepository.findByReportIdOrderByCreateTimeDescIdDesc(reportId, one);
        if (any != null && !any.isEmpty()) {
            return any.get(0);
        }
        return null;
    }

    /**
     * runs 须已按 create_time DESC, id DESC 排序（同 report 内）；取第一条。
     */
    private SimpleReportRun pickDisplayRun(List<SimpleReportRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return null;
        }
        for (SimpleReportRun run : runs) {
            if (run != null) {
                return run;
            }
        }
        return null;
    }

    private void putDisplayRunFields(Map<String, Object> m, SimpleReportRun displayRun) {
        if (displayRun == null) {
            m.put("latestRunStatus", null);
            m.put("latestRunId", null);
            m.put("latestFailMessage", null);
            return;
        }
        m.put("latestRunStatus", displayRun.getRunStatus());
        m.put("latestRunId", displayRun.getId());
        if (displayRun.getRunStatus() != null
                && displayRun.getRunStatus().intValue() == SimpleReportRun.STATUS_FAILED) {
            m.put("latestFailMessage", displayRun.getFailMessage());
        } else {
            m.put("latestFailMessage", null);
        }
    }

    private boolean canDownload(SimpleReportRun run) {
        if (run == null) {
            return false;
        }
        if (run.getRunStatus() == null || run.getRunStatus() != SimpleReportRun.STATUS_SUCCESS) {
            return false;
        }
        String path = resolveStoredPath(run);
        if (StringUtils.isBlank(path)) {
            return false;
        }
        File file = new File(path.trim());
        if (!file.isFile() || !file.exists()) {
            return false;
        }
        try {
            assertUnderAllowedRoot(file, run.getId());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String resolveStoredPath(SimpleReportRun run) {
        if (StringUtils.isNotBlank(run.getDeliveryPath())) {
            return run.getDeliveryPath();
        }
        return run.getFilePath();
    }

    private void assertUnderAllowedRoot(File file, Long runId) {
        try {
            String canonical = file.getCanonicalPath();
            if (isUnderBase(canonical, FileUtil.fileConfig.getPrefixFilePhysicalPath())) {
                return;
            }
            String deliveryRoot = properties.getDeliveryRoot();
            if (StringUtils.isBlank(deliveryRoot)) {
                deliveryRoot = CommonUtil.getBasePath("report") + "simple-delivery";
            }
            if (isUnderBase(canonical, deliveryRoot)) {
                return;
            }
            logger.warn("simple-report download path outside root, runId=" + runId + ", path=" + canonical);
            throw new IllegalArgumentException("文件路径非法");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("simple-report download path check failed, runId=" + runId, e);
            throw new IllegalArgumentException("文件不可用");
        }
    }

    private static boolean isUnderBase(String canonical, String base) {
        if (StringUtils.isBlank(base) || StringUtils.isBlank(canonical)) {
            return false;
        }
        try {
            String baseCanon = new File(base).getCanonicalPath();
            return canonical.equals(baseCanon)
                    || canonical.startsWith(baseCanon + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    void onGenerateSuccess(Integer engineReportId, String filePath) {
        Optional<SimpleReportRun> opt = runRepository.findByEngineReportId(engineReportId);
        if (!opt.isPresent()) {
            return;
        }
        SimpleReportRun run = opt.get();
        if (run.getRunStatus() == null || run.getRunStatus() != SimpleReportRun.STATUS_GENERATING) {
            return;
        }
        run.setFilePath(filePath);
        try {
            String delivery = copyToDelivery(run, filePath);
            run.setDeliveryPath(delivery);
        } catch (Exception e) {
            logger.error("copy to delivery failed, runId=" + run.getId(), e);
            run.setRunStatus(SimpleReportRun.STATUS_FAILED);
            run.setFailMessage(truncate("生成成功但交付失败：" + e.getMessage(), 512));
            run.setFinishedTime(LocalDateTime.now());
            runRepository.save(run);
            return;
        }
        run.setRunStatus(SimpleReportRun.STATUS_SUCCESS);
        run.setFailMessage(null);
        run.setFinishedTime(LocalDateTime.now());
        runRepository.save(run);

        try {
            JSONObject snapshot = JSON.parseObject(run.getConfigSnapshotJson());
            String notifyEmail = snapshot == null ? null : snapshot.getString("notifyEmail");
            String attachment = StringUtils.isNotBlank(run.getDeliveryPath())
                    ? run.getDeliveryPath()
                    : filePath;
            String mailErr = mailService.sendReportAttachment(notifyEmail, run.getReportName(), attachment);
            if (mailErr != null && SimpleReportMailService.parseEmails(notifyEmail).size() > 0) {
                logger.warn("mail after generate: " + mailErr + ", runId=" + run.getId());
                // 不改 SUCCESS；把发信结果记在 failMessage 仅作提示（可选）
                run.setFailMessage(truncate("报告已生成；" + mailErr, 512));
                runRepository.save(run);
            }
        } catch (Exception e) {
            logger.error("mail notify failed, runId=" + run.getId(), e);
        }
    }

    void onGenerateFailure(Integer engineReportId, String message) {
        Optional<SimpleReportRun> opt = runRepository.findByEngineReportId(engineReportId);
        if (!opt.isPresent()) {
            return;
        }
        SimpleReportRun run = opt.get();
        if (run.getRunStatus() == null || run.getRunStatus() != SimpleReportRun.STATUS_GENERATING) {
            return;
        }
        run.setRunStatus(SimpleReportRun.STATUS_FAILED);
        run.setFailMessage(truncate(message, 512));
        run.setFinishedTime(LocalDateTime.now());
        runRepository.save(run);
    }

    private String copyToDelivery(SimpleReportRun run, String filePath) throws Exception {
        JSONObject snapshot = JSON.parseObject(run.getConfigSnapshotJson());
        String deliveryDir = snapshot.getString("deliveryDir");
        Path destDir = resolveDeliveryDir(deliveryDir);
        Files.createDirectories(destDir);
        String ts = LocalDateTime.now().format(FILE_TS);
        String fileName = sanitizeFileName(run.getReportName()) + "_" + run.getId() + "_" + ts + ".docx";
        Path dest = destDir.resolve(fileName);
        Files.copy(Paths.get(filePath), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toAbsolutePath().toString();
    }

    private Path resolveDeliveryDir(String deliveryDir) {
        String relative = requireDeliveryDir(deliveryDir);
        String root = properties.getDeliveryRoot();
        if (StringUtils.isBlank(root)) {
            root = CommonUtil.getBasePath("report") + "simple-delivery";
        }
        Path rootPath = Paths.get(root).toAbsolutePath().normalize();
        Path dest = rootPath.resolve(relative).normalize();
        if (!dest.startsWith(rootPath)) {
            throw new IllegalArgumentException("交付目录越界");
        }
        return dest;
    }

    private int replaceBlocks(Long reportId, List<Object> blocks) {
        if (blocks.size() > MAX_BLOCKS) {
            throw new IllegalArgumentException("区块数量不能超过 " + MAX_BLOCKS);
        }
        blockRepository.deleteByReportId(reportId);
        int order = 0;
        for (Object o : blocks) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("blocks 项须为对象");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            SimpleReportBlock b = parseBlock(m, reportId, order++);
            httpFetcher.validateUrl(b.getUrl());
            blockRepository.save(b);
        }
        return order;
    }

    private SimpleReportBlock parseBlock(Map<String, Object> m, Long reportId, int order) {
        String title = str(m.get("title"));
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("区块标题不能为空");
        }
        if (title.length() > 128) {
            throw new IllegalArgumentException("区块标题过长");
        }
        String method = str(m.get("httpMethod"));
        if (StringUtils.isBlank(method)) {
            method = SimpleReportBlock.METHOD_GET;
        }
        method = method.trim().toUpperCase();
        if (!SimpleReportBlock.METHOD_GET.equals(method) && !SimpleReportBlock.METHOD_POST.equals(method)) {
            throw new IllegalArgumentException("httpMethod 须为 GET 或 POST");
        }
        String url = str(m.get("url"));
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url 不能为空");
        }
        if (url.length() > 1024) {
            throw new IllegalArgumentException("url 过长");
        }
        String style = str(m.get("renderStyle"));
        if (StringUtils.isBlank(style)) {
            throw new IllegalArgumentException("renderStyle 不能为空");
        }
        style = style.trim().toUpperCase();
        if (!SimpleReportBlock.STYLE_TABLE.equals(style)
                && !SimpleReportBlock.STYLE_BAR.equals(style)
                && !SimpleReportBlock.STYLE_PIE.equals(style)
                && !SimpleReportBlock.STYLE_LINE.equals(style)) {
            throw new IllegalArgumentException("renderStyle 须为 TABLE/BAR/PIE/LINE");
        }
        SimpleReportBlock b = new SimpleReportBlock();
        b.setReportId(reportId);
        b.setSortOrder(order);
        b.setTitle(title.trim());
        b.setHttpMethod(method);
        b.setUrl(url.trim());
        b.setQueryJson(jsonFieldToString(m.get("queryJson")));
        b.setBodyJson(jsonFieldToString(m.get("bodyJson")));
        b.setRenderStyle(style);
        String hint = str(m.get("promptHint"));
        if (StringUtils.isNotBlank(hint) && hint.length() > 512) {
            throw new IllegalArgumentException("promptHint 过长");
        }
        b.setPromptHint(StringUtils.isBlank(hint) ? null : hint.trim());
        return b;
    }

    private JSONObject buildSnapshot(SimpleReport r, List<SimpleReportBlock> blocks) {
        JSONObject snap = new JSONObject(true);
        snap.put("reportId", r.getId());
        snap.put("name", r.getName());
        snap.put("deliveryDir", r.getDeliveryDir());
        snap.put("notifyEmail", r.getNotifyEmail());
        JSONArray arr = new JSONArray();
        for (SimpleReportBlock b : blocks) {
            JSONObject o = new JSONObject(true);
            o.put("title", b.getTitle());
            o.put("httpMethod", b.getHttpMethod());
            o.put("url", b.getUrl());
            o.put("queryJson", b.getQueryJson());
            o.put("bodyJson", b.getBodyJson());
            o.put("renderStyle", b.getRenderStyle());
            o.put("promptHint", b.getPromptHint());
            arr.add(o);
        }
        snap.put("blocks", arr);
        return snap;
    }

    private String buildPlanMd(SimpleReport r, List<SimpleReportBlock> blocks, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 简化报告计划：").append(r.getName()).append("\n\n");
        sb.append("## 诉求\n");
        sb.append(StringUtils.isBlank(userPrompt) ? "（无）" : userPrompt).append("\n\n");
        int i = 1;
        for (SimpleReportBlock b : blocks) {
            sb.append("## ").append(i++).append(". ").append(b.getTitle()).append("\n");
            String purpose = StringUtils.isNotBlank(b.getPromptHint()) ? b.getPromptHint() : b.getTitle();
            sb.append("- 标题：").append(b.getTitle()).append("\n");
            sb.append("- 样式：").append(b.getRenderStyle()).append("\n");
            sb.append("- 查询目的：").append(purpose).append("\n");
            sb.append("- 接口：").append(b.getHttpMethod()).append(" ").append(b.getUrl()).append("\n");
            sb.append("- 预期数据：labels/values\n\n");
        }
        sb.append("## 交付\n");
        sb.append("- 目录：").append(r.getDeliveryDir()).append("\n");
        if (StringUtils.isNotBlank(r.getNotifyEmail())) {
            sb.append("- 邮件：").append(r.getNotifyEmail()).append("\n");
        } else {
            sb.append("- 邮件：未配置收件人（不发送）\n");
        }
        return sb.toString();
    }

    private Map<String, Object> toBlockMap(SimpleReportBlock b) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", b.getId());
        m.put("sortOrder", b.getSortOrder());
        m.put("title", b.getTitle());
        m.put("httpMethod", b.getHttpMethod());
        m.put("url", b.getUrl());
        m.put("queryJson", b.getQueryJson());
        m.put("bodyJson", b.getBodyJson());
        m.put("renderStyle", b.getRenderStyle());
        m.put("promptHint", b.getPromptHint());
        return m;
    }

    private Map<String, Object> toRunMap(SimpleReportRun run, boolean withPlan) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("runId", run.getId());
        m.put("reportId", run.getReportId());
        m.put("reportName", run.getReportName());
        m.put("userPrompt", run.getUserPrompt());
        if (withPlan) {
            m.put("planMd", run.getPlanMd());
        }
        m.put("runStatus", run.getRunStatus());
        m.put("canDownload", canDownload(run));
        m.put("engineReportId", run.getEngineReportId());
        m.put("warnCount", run.getWarnCount());
        m.put("filePath", run.getFilePath());
        m.put("deliveryPath", run.getDeliveryPath());
        m.put("failMessage", run.getFailMessage());
        m.put("triggerType", run.getTriggerType());
        m.put("confirmedTime", run.getConfirmedTime());
        m.put("finishedTime", run.getFinishedTime());
        m.put("createTime", run.getCreateTime());
        m.put("updateTime", run.getUpdateTime());
        return m;
    }

    private SimpleReport requireReport(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        Optional<SimpleReport> opt = reportRepository.findByIdAndDeleted(id, NOT_DELETED);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("报告不存在或已删除");
        }
        return opt.get();
    }

    private SimpleReportRun requireRun(Long runId) {
        if (runId == null) {
            throw new IllegalArgumentException("runId 不能为空");
        }
        Optional<SimpleReportRun> opt = runRepository.findById(runId);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("运行记录不存在");
        }
        return opt.get();
    }

    private String requireName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        String n = name.trim();
        if (n.length() > 128) {
            throw new IllegalArgumentException("名称过长");
        }
        return n;
    }

    private String requireDeliveryDir(String deliveryDir) {
        if (StringUtils.isBlank(deliveryDir)) {
            throw new IllegalArgumentException("deliveryDir 不能为空");
        }
        String d = deliveryDir.trim().replace('\\', '/');
        if (d.contains("..") || d.startsWith("/") || d.contains(":")) {
            throw new IllegalArgumentException("deliveryDir 非法");
        }
        if (d.length() > 512) {
            throw new IllegalArgumentException("deliveryDir 过长");
        }
        while (d.startsWith("./")) {
            d = d.substring(2);
        }
        if (StringUtils.isBlank(d)) {
            throw new IllegalArgumentException("deliveryDir 不能为空");
        }
        return d;
    }

    private String normalizeNotifyEmail(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        List<String> list = SimpleReportMailService.parseEmails(raw);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("notifyEmail 格式不正确");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(list.get(i));
        }
        if (sb.length() > 512) {
            throw new IllegalArgumentException("notifyEmail 过长");
        }
        return sb.toString();
    }

    private static String jsonFieldToString(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            return StringUtils.isBlank(s) ? null : s;
        }
        if (v instanceof Map || v instanceof List) {
            return JSON.toJSONString(v);
        }
        return String.valueOf(v);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int toInt(Object v, int def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String sanitizeFileName(String name) {
        if (StringUtils.isBlank(name)) {
            return "simple-report";
        }
        String s = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (s.length() > 80) {
            s = s.substring(0, 80);
        }
        return s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }
}
