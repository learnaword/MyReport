package com.myreport.service;

import com.myreport.entity.GradEmploymentRecord;
import com.myreport.framework.redis.ImportProgressUtil;
import com.myreport.repository.GradEmploymentRecordRepository;
import com.myreport.util.excel.GradEmploymentExcelUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 毕业生就业去向：列表与 Excel 导入（默认归属武汉大学；学校ID+学号+毕业年份覆盖更新）。
 */
@Service
public class GradEmploymentService {

    private static final Logger logger = Logger.getLogger(GradEmploymentService.class);

    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private static final String[] IGNORE_COPY = new String[] {"id", "createdAt", "updatedAt", "importSchoolName"};

    private final GradEmploymentRecordRepository repository;
    private final DefaultSchoolService defaultSchoolService;

    public GradEmploymentService(GradEmploymentRecordRepository repository,
                                 DefaultSchoolService defaultSchoolService) {
        this.repository = repository;
        this.defaultSchoolService = defaultSchoolService;
    }

    /**
     * 列表仅返回默认校数据；{@code schoolId} 入参忽略。
     */
    public Page<GradEmploymentRecord> list(int page, int size, Long schoolId) {
        int safePage = page < 0 ? 0 : page;
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Long defaultId = defaultSchoolService.requireId();
        return repository.findBySchoolId(defaultId, pageable);
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("文件大小不能超过 20MB");
        }
        validateFilename(file.getOriginalFilename());
    }

    /**
     * 兼容旧调用：选校入参已忽略。
     */
    public void validateDefaultSchoolId(Long schoolId) {
        // schoolId ignored — always use default school
    }

    @Transactional
    public Map<String, Object> importFromExcel(File file, String filename, Long schoolId, String taskId)
            throws Exception {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return importFromExcel(in, filename, schoolId, taskId);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    /**
     * 导入：忽略请求 schoolId 与 Excel 学校列，一律归属武汉大学。
     */
    @Transactional
    public Map<String, Object> importFromExcel(InputStream in, String filename, Long schoolId, String taskId)
            throws Exception {
        Long defaultId = defaultSchoolService.requireId();

        String batchId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        if (taskId != null) {
            ImportProgressUtil.update(taskId, 8, 100, "正在解析 Excel…");
        }
        List<GradEmploymentRecord> parsed = GradEmploymentExcelUtil.parse(
                in,
                filename,
                batchId,
                defaultId
        );

        int inserted = 0;
        int updated = 0;
        int total = parsed.size();
        for (int i = 0; i < parsed.size(); i++) {
            GradEmploymentRecord incoming = parsed.get(i);
            incoming.setSchoolId(defaultId);
            Optional<GradEmploymentRecord> existingOpt = repository
                    .findBySchoolIdAndStudentNoAndGraduationYear(
                            incoming.getSchoolId(),
                            incoming.getStudentNo(),
                            incoming.getGraduationYear());
            if (existingOpt.isPresent()) {
                GradEmploymentRecord existing = existingOpt.get();
                BeanUtils.copyProperties(incoming, existing, IGNORE_COPY);
                repository.save(existing);
                updated++;
            } else {
                repository.save(incoming);
                inserted++;
            }
            if (taskId != null && total > 0 && (i % 20 == 0 || i + 1 == total)) {
                int pct = 15 + (int) ((i + 1) * 80L / total);
                ImportProgressUtil.update(taskId, pct, 100,
                        "正在写入 " + (i + 1) + "/" + total);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("batchId", batchId);
        summary.put("totalParsed", parsed.size());
        summary.put("inserted", inserted);
        summary.put("updated", updated);
        logger.info("grad employment import done, batchId=" + batchId
                + ", defaultSchoolId=" + defaultId
                + ", inserted=" + inserted + ", updated=" + updated);
        return summary;
    }

    private void validateFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("文件名无效");
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".xlsx") || lower.endsWith(".xls"))) {
            throw new IllegalArgumentException("仅支持 .xlsx / .xls 文件");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("文件名非法");
        }
    }
}
