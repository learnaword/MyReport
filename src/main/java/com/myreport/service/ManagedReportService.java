package com.myreport.service;

import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.ManagedReport;
import com.myreport.entity.ReportTemplate;
import com.myreport.framework.redis.RedisTemplate;
import com.myreport.repository.ManagedReportRepository;
import com.myreport.repository.ReportTemplateRepository;
import com.myreport.util.Constant;
import com.myreport.util.FileUtil;
import com.myreport.util.word.SpireReportUtil;
import com.myreport.vo.CreateReportVO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 报告实例：CRUD + 一键生成（学校固定为武汉大学）。
 */
@Service
public class ManagedReportService {

    private static final Logger logger = Logger.getLogger(ManagedReportService.class);

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final int LOCK_TTL_SECONDS = 2 * 60 * 60;

    private final ManagedReportRepository reportRepository;
    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateService reportTemplateService;
    private final MetricAggregateService metricAggregateService;
    private final ReportJsonAssembler reportJsonAssembler;
    private final DefaultSchoolService defaultSchoolService;

    public ManagedReportService(ManagedReportRepository reportRepository,
                                ReportTemplateRepository templateRepository,
                                ReportTemplateService reportTemplateService,
                                MetricAggregateService metricAggregateService,
                                ReportJsonAssembler reportJsonAssembler,
                                DefaultSchoolService defaultSchoolService) {
        this.reportRepository = reportRepository;
        this.templateRepository = templateRepository;
        this.reportTemplateService = reportTemplateService;
        this.metricAggregateService = metricAggregateService;
        this.reportJsonAssembler = reportJsonAssembler;
        this.defaultSchoolService = defaultSchoolService;
    }

    public Page<ManagedReport> list(int page, int size) {
        int safePage = page < 0 ? 0 : page;
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        return reportRepository.findByDeleted(
                NOT_DELETED,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updateTime")));
    }

    public Map<String, Object> toListItem(ManagedReport r) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("schoolId", r.getSchoolId());
        m.put("templateId", r.getTemplateId());
        m.put("generateStatus", r.getGenerateStatus());
        m.put("filePath", r.getFilePath());
        m.put("failMessage", r.getFailMessage());
        m.put("lastGenerateTime", r.getLastGenerateTime());
        m.put("createTime", r.getCreateTime());
        m.put("updateTime", r.getUpdateTime());
        m.put("canDownload", canDownload(r));
        m.put("schoolName", DefaultSchoolService.DEFAULT_SCHOOL_NAME);
        Optional<ReportTemplate> tpl = templateRepository.findById(r.getTemplateId());
        if (tpl.isPresent() && Integer.valueOf(NOT_DELETED).equals(tpl.get().getDeleted())) {
            m.put("templateName", tpl.get().getName());
        } else {
            m.put("templateName", null);
        }
        return m;
    }

    /**
     * 解析可下载文件：仅按报告 ID 取库内登记路径，禁止客户端传路径。
     */
    public DownloadPayload prepareDownload(Long id) {
        ManagedReport r = requireReport(id);
        Integer st = r.getGenerateStatus();
        if (st == null
                || st == ManagedReport.STATUS_DRAFT
                || st == ManagedReport.STATUS_GENERATING) {
            throw new IllegalArgumentException("报告尚未生成成功，无法下载");
        }
        if (StringUtils.isBlank(r.getFilePath())) {
            throw new IllegalArgumentException("暂无可下载文件");
        }
        if (st != ManagedReport.STATUS_SUCCESS && st != ManagedReport.STATUS_FAILED) {
            throw new IllegalArgumentException("当前状态不可下载");
        }

        File file = new File(r.getFilePath().trim());
        try {
            String canonical = file.getCanonicalPath();
            String base = FileUtil.fileConfig.getPrefixFilePhysicalPath();
            if (StringUtils.isNotBlank(base)) {
                String baseCanon = new File(base).getCanonicalPath();
                if (!canonical.equals(baseCanon)
                        && !canonical.startsWith(baseCanon + File.separator)) {
                    logger.warn("download path outside base, id=" + id + ", path=" + canonical);
                    throw new IllegalArgumentException("文件路径非法");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("download path check failed, id=" + id, e);
            throw new IllegalArgumentException("文件不可用");
        }
        if (!file.isFile() || !file.exists()) {
            throw new IllegalArgumentException("文件不存在或已失效，请重新生成");
        }

        String downloadName = sanitizeFileName(r.getName());
        if (!downloadName.toLowerCase().endsWith(".docx")) {
            downloadName = downloadName + ".docx";
        }
        return new DownloadPayload(file, downloadName);
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

    private static boolean canDownload(ManagedReport r) {
        if (r == null) {
            return false;
        }
        Integer st = r.getGenerateStatus();
        if (st == null
                || st == ManagedReport.STATUS_DRAFT
                || st == ManagedReport.STATUS_GENERATING) {
            return false;
        }
        if (StringUtils.isBlank(r.getFilePath())) {
            return false;
        }
        return st == ManagedReport.STATUS_SUCCESS || st == ManagedReport.STATUS_FAILED;
    }

    public Map<String, Object> detail(Long id) {
        ManagedReport r = requireReport(id);
        return toListItem(r);
    }

    /**
     * 创建报告。{@code schoolId} 入参忽略，写入默认校；同模版仅一份未删。
     */
    @Transactional
    public Long create(String name, Long schoolId, Long templateId) {
        String trimmed = requireName(name);
        Long defaultSchoolId = defaultSchoolService.requireId();
        requireEnabledTemplate(templateId);
        if (reportRepository.existsByTemplateIdAndDeleted(templateId, NOT_DELETED)) {
            throw new IllegalArgumentException("该模版已存在报告");
        }
        ManagedReport r = new ManagedReport();
        r.setName(trimmed);
        r.setSchoolId(defaultSchoolId);
        r.setTemplateId(templateId);
        r.setGenerateStatus(ManagedReport.STATUS_DRAFT);
        r.setDeleted(NOT_DELETED);
        reportRepository.save(r);
        return r.getId();
    }

    /**
     * 更新报告。{@code schoolId} 忽略；校正为默认校；模版维度唯一。
     */
    @Transactional
    public void update(Long id, String name, Long schoolId, Long templateId) {
        ManagedReport r = requireReport(id);
        if (r.getGenerateStatus() != null
                && r.getGenerateStatus() == ManagedReport.STATUS_GENERATING) {
            throw new IllegalArgumentException("报告生成中，暂不可修改");
        }
        if (name != null) {
            r.setName(requireName(name));
        }
        Long defaultSchoolId = defaultSchoolService.requireId();
        Long nextTemplate = templateId != null ? templateId : r.getTemplateId();
        if (templateId != null) {
            requireEnabledTemplate(templateId);
        }
        if (!nextTemplate.equals(r.getTemplateId())) {
            if (reportRepository.existsByTemplateIdAndDeletedAndIdNot(
                    nextTemplate, NOT_DELETED, id)) {
                throw new IllegalArgumentException("该模版已存在报告");
            }
            r.setTemplateId(nextTemplate);
        }
        r.setSchoolId(defaultSchoolId);
        reportRepository.save(r);
    }

    @Transactional
    public void delete(Long id) {
        ManagedReport r = requireReport(id);
        if (r.getGenerateStatus() != null
                && r.getGenerateStatus() == ManagedReport.STATUS_GENERATING) {
            throw new IllegalArgumentException("报告生成中，暂不可删除");
        }
        String path = r.getFilePath();
        r.setDeleted(DELETED);
        r.setDeleteTime(LocalDateTime.now());
        r.setFilePath(null);
        reportRepository.save(r);
        deleteFileQuietly(path);
    }

    /**
     * 组装数据并提交异步 Word 生成（按默认校聚合）。
     */
    @Transactional
    public void generate(Long id) {
        ManagedReport r = requireReport(id);
        if (r.getGenerateStatus() != null
                && r.getGenerateStatus() == ManagedReport.STATUS_GENERATING) {
            throw new IllegalArgumentException("报告正在生成中");
        }
        Long defaultSchoolId = defaultSchoolService.requireId();
        if (!defaultSchoolId.equals(r.getSchoolId())) {
            r.setSchoolId(defaultSchoolId);
            reportRepository.save(r);
        }
        ReportTemplate template = requireEnabledTemplate(r.getTemplateId());

        Map<String, Object> detail = reportTemplateService.detail(template.getId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = detail.get("nodes") instanceof List
                ? (List<Map<String, Object>>) detail.get("nodes")
                : new ArrayList<Map<String, Object>>();

        List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();
        Map<Long, String> intros = new HashMap<Long, String>();
        reportJsonAssembler.collectMetrics(nodes, metrics, intros);
        Map<Long, JSONObject> metricData = metricAggregateService.aggregate(defaultSchoolId, metrics, intros);
        ReportJsonAssembler.AssembleResult assembled = reportJsonAssembler.assemble(nodes, metricData);
        if (assembled.getReportJsonArr() == null || assembled.getReportJsonArr().isEmpty()) {
            throw new IllegalArgumentException("无可生成的报告内容（请检查模版指标与学校数据）");
        }
        if (assembled.getMetricCount() <= 0) {
            throw new IllegalArgumentException("没有可计算的指标数据");
        }

        if (r.getId() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("报告 ID 超出生成侧限制");
        }
        Integer reportId = r.getId().intValue();
        String lockKey = String.format(Constant.RedisKey.REPORT_LOCK, reportId);
        if (!RedisTemplate.setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS)) {
            throw new IllegalArgumentException("报告正在生成中，请稍后再试");
        }

        r.setGenerateStatus(ManagedReport.STATUS_GENERATING);
        r.setFailMessage(null);
        r.setLastGenerateTime(LocalDateTime.now());
        reportRepository.save(r);

        try {
            JSONObject overallSetting = new JSONObject(true);
            overallSetting.put("strCoverImg", nullToEmpty(template.getCoverImage()));
            overallSetting.put("strBackCoverImg", nullToEmpty(template.getBackCoverImage()));
            overallSetting.put("nMetricsCount", Math.max(assembled.getMetricCount(), 1));
            overallSetting.put("managedReportSync", true);

            CreateReportVO vo = new CreateReportVO();
            vo.setReportId(reportId);
            vo.setReportName(sanitizeFileName(r.getName()));
            vo.setLSchoolId(defaultSchoolId);
            vo.setReportJsonArr(assembled.getReportJsonArr());
            vo.setOverallSetting(overallSetting);
            vo.set("managedReportSync", true);

            SpireReportUtil.createReport(assembled.getReportJsonArr(), overallSetting, vo);
        } catch (Exception e) {
            logger.error("submit generate failed, id=" + id, e);
            RedisTemplate.delete(lockKey);
            r.setGenerateStatus(ManagedReport.STATUS_FAILED);
            r.setFailMessage(truncate("提交生成失败：" + e.getMessage(), 512));
            reportRepository.save(r);
            throw new IllegalArgumentException("提交生成失败：" + e.getMessage());
        }
    }

    private ManagedReport requireReport(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        Optional<ManagedReport> opt = reportRepository.findByIdAndDeleted(id, NOT_DELETED);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("报告不存在或已删除");
        }
        return opt.get();
    }

    private ReportTemplate requireEnabledTemplate(Long templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("模版不能为空");
        }
        Optional<ReportTemplate> opt = templateRepository.findById(templateId);
        if (!opt.isPresent() || !Integer.valueOf(NOT_DELETED).equals(opt.get().getDeleted())) {
            throw new IllegalArgumentException("模版不存在或已删除");
        }
        ReportTemplate t = opt.get();
        if (t.getStatus() == null || t.getStatus() != 1) {
            throw new IllegalArgumentException("模版已停用，无法使用");
        }
        return t;
    }

    private static String requireName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("报告名称不能为空");
        }
        String t = name.trim();
        if (t.length() > 128) {
            throw new IllegalArgumentException("报告名称不能超过 128 字");
        }
        return t;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "report";
        }
        String s = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (s.isEmpty()) {
            return "report";
        }
        if (s.length() > 80) {
            return s.substring(0, 80);
        }
        return s;
    }

    private static void deleteFileQuietly(String path) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        try {
            File f = new File(path);
            if (f.isFile() && !f.delete()) {
                logger.warn("failed to delete report file: " + path);
            }
        } catch (Exception e) {
            logger.warn("delete report file error: " + path, e);
        }
    }
}
