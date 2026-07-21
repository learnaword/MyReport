package com.myreport.service;

import com.myreport.entity.GradEmploymentRecord;
import com.myreport.entity.School;
import com.myreport.framework.redis.ImportProgressUtil;
import com.myreport.repository.GradEmploymentRecordRepository;
import com.myreport.repository.SchoolRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * 毕业生就业去向：列表与 Excel 导入（学校ID+学号+毕业年份覆盖更新）。
 */
@Service
public class GradEmploymentService {

    private static final Logger logger = Logger.getLogger(GradEmploymentService.class);

    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private static final String[] IGNORE_COPY = new String[] {"id", "createdAt", "updatedAt", "importSchoolName"};

    private final GradEmploymentRecordRepository repository;
    private final SchoolRepository schoolRepository;
    private final Random random = new Random();

    public GradEmploymentService(GradEmploymentRecordRepository repository, SchoolRepository schoolRepository) {
        this.repository = repository;
        this.schoolRepository = schoolRepository;
    }

    public Page<GradEmploymentRecord> list(int page, int size, Long schoolId) {
        int safePage = page < 0 ? 0 : page;
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        if (schoolId != null) {
            return repository.findBySchoolId(schoolId, pageable);
        }
        return repository.findAll(pageable);
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

    public void validateDefaultSchoolId(Long schoolId) {
        if (schoolId != null && !schoolRepository.existsById(schoolId)) {
            throw new IllegalArgumentException("学校不存在：" + schoolId);
        }
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

    @Transactional
    public Map<String, Object> importFromExcel(InputStream in, String filename, Long schoolId, String taskId)
            throws Exception {
        if (schoolId != null && !schoolRepository.existsById(schoolId)) {
            throw new IllegalArgumentException("学校不存在：" + schoolId);
        }

        String batchId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        if (taskId != null) {
            ImportProgressUtil.update(taskId, 8, 100, "正在解析 Excel…");
        }
        List<GradEmploymentRecord> parsed = GradEmploymentExcelUtil.parse(
                in,
                filename,
                batchId,
                schoolId
        );

        Map<String, Long> schoolNameToId = buildSchoolNameCache();
        Long fallbackSchoolId = null;

        int inserted = 0;
        int updated = 0;
        int total = parsed.size();
        for (int i = 0; i < parsed.size(); i++) {
            GradEmploymentRecord incoming = parsed.get(i);
            resolveSchoolId(incoming, schoolNameToId);
            if (incoming.getSchoolId() == null) {
                if (fallbackSchoolId == null) {
                    fallbackSchoolId = pickRandomSchoolId();
                }
                incoming.setSchoolId(fallbackSchoolId);
            }
            if (!schoolRepository.existsById(incoming.getSchoolId())) {
                throw new IllegalArgumentException("学校不存在：" + incoming.getSchoolId()
                        + "（学号 " + incoming.getStudentNo() + "）");
            }
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
                + ", inserted=" + inserted + ", updated=" + updated);
        return summary;
    }

    /**
     * 若尚无 schoolId，则按 Excel「学校名称」精确匹配 school.name。
     */
    private void resolveSchoolId(GradEmploymentRecord incoming, Map<String, Long> schoolNameToId) {
        if (incoming.getSchoolId() != null) {
            return;
        }
        String name = incoming.getImportSchoolName();
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String key = name.trim();
        Long id = schoolNameToId.get(key);
        if (id == null) {
            Optional<School> found = schoolRepository.findFirstByName(key);
            if (!found.isPresent()) {
                throw new IllegalArgumentException("学校名称不存在：" + key
                        + "（学号 " + incoming.getStudentNo() + "）");
            }
            id = found.get().getId();
            schoolNameToId.put(key, id);
        }
        incoming.setSchoolId(id);
    }

    private Map<String, Long> buildSchoolNameCache() {
        Map<String, Long> map = new HashMap<String, Long>();
        for (School school : schoolRepository.findAll()) {
            if (school.getName() != null && !school.getName().isEmpty() && school.getId() != null) {
                if (!map.containsKey(school.getName())) {
                    map.put(school.getName(), school.getId());
                }
            }
        }
        return map;
    }

    private Long pickRandomSchoolId() {
        List<School> schools = schoolRepository.findAll();
        if (schools == null || schools.isEmpty()) {
            throw new IllegalArgumentException("数据库中没有学校，无法导入；请先导入学校或指定学校ID/学校名称");
        }
        School picked = schools.get(random.nextInt(schools.size()));
        logger.info("import without school, randomly picked schoolId=" + picked.getId()
                + ", name=" + picked.getName());
        return picked.getId();
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
