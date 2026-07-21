package com.myreport.service;

import com.myreport.entity.School;
import com.myreport.repository.SchoolRepository;
import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 单校默认：武汉大学（按名称查找/自动创建，不写死主键）。
 */
@Service
public class DefaultSchoolService {

    private static final Logger logger = Logger.getLogger(DefaultSchoolService.class);

    public static final String DEFAULT_SCHOOL_NAME = "武汉大学";

    private final SchoolRepository schoolRepository;

    public DefaultSchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    /**
     * 确保默认校存在并返回实体。
     */
    @Transactional
    public School getOrCreate() {
        Optional<School> found = schoolRepository.findFirstByName(DEFAULT_SCHOOL_NAME);
        if (found.isPresent()) {
            return found.get();
        }
        try {
            School school = new School();
            school.setName(DEFAULT_SCHOOL_NAME);
            School saved = schoolRepository.save(school);
            logger.info("created default school id=" + saved.getId() + ", name=" + DEFAULT_SCHOOL_NAME);
            return saved;
        } catch (DataIntegrityViolationException e) {
            Optional<School> again = schoolRepository.findFirstByName(DEFAULT_SCHOOL_NAME);
            if (again.isPresent()) {
                return again.get();
            }
            logger.error("create default school conflict but row not found", e);
            throw new IllegalStateException("无法创建默认学校：" + DEFAULT_SCHOOL_NAME);
        }
    }

    /**
     * 默认校主键。
     */
    @Transactional
    public Long requireId() {
        School school = getOrCreate();
        if (school.getId() == null) {
            throw new IllegalStateException("默认学校 ID 为空");
        }
        return school.getId();
    }
}
