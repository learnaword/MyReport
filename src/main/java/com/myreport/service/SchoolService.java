package com.myreport.service;

import com.myreport.entity.School;
import com.myreport.framework.redis.ImportProgressUtil;
import com.myreport.repository.SchoolRepository;
import com.myreport.util.excel.SchoolExcelUtil;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 学校业务：列表与 Excel 导入。
 */
@Service
public class SchoolService {

    private static final Logger logger = Logger.getLogger(SchoolService.class);

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public Page<School> list(int page, int size) {
        int safePage = page < 0 ? 0 : page;
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        return schoolRepository.findAll(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("文件大小不能超过 5MB");
        }
        validateFilename(file.getOriginalFilename());
    }

    /**
     * 从 Excel 导入学校名称，逐行新增。可传入 taskId 回写 Redis 进度。
     */
    @Transactional
    public Map<String, Object> importFromExcel(InputStream in, String filename, String taskId) throws Exception {
        if (taskId != null) {
            ImportProgressUtil.update(taskId, 10, 100, "正在解析 Excel…");
        }
        List<String> names = SchoolExcelUtil.parseSchoolNames(in, filename);
        if (taskId != null) {
            ImportProgressUtil.update(taskId, 50, 100, "正在写入数据库…");
        }

        List<School> schools = new ArrayList<School>(names.size());
        for (String name : names) {
            School school = new School();
            school.setName(name);
            schools.add(school);
        }

        schoolRepository.saveAll(schools);
        if (taskId != null) {
            ImportProgressUtil.update(taskId, 95, 100, "写入完成");
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("successCount", schools.size());
        summary.put("totalParsed", names.size());
        logger.info("school excel import done, count=" + schools.size());
        return summary;
    }

    @Transactional
    public Map<String, Object> importFromExcel(File file, String filename, String taskId) throws Exception {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return importFromExcel(in, filename, taskId);
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
