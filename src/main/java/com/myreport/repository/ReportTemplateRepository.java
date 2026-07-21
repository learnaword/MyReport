package com.myreport.repository;

import com.myreport.entity.ReportTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 报告模版仓储。
 */
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    Optional<ReportTemplate> findByIdAndDeleted(Long id, Integer deleted);

    Page<ReportTemplate> findByDeleted(Integer deleted, Pageable pageable);

    Page<ReportTemplate> findByDeletedAndStatus(Integer deleted, Integer status, Pageable pageable);

    Page<ReportTemplate> findByDeletedAndNameContaining(Integer deleted, String name, Pageable pageable);

    Page<ReportTemplate> findByDeletedAndStatusAndNameContaining(
            Integer deleted, Integer status, String name, Pageable pageable);
}
