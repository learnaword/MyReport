package com.myreport.repository;

import com.myreport.entity.ManagedReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 报告实例仓储。
 */
public interface ManagedReportRepository extends JpaRepository<ManagedReport, Long> {

    Page<ManagedReport> findByDeleted(Integer deleted, Pageable pageable);

    Optional<ManagedReport> findByIdAndDeleted(Long id, Integer deleted);

    boolean existsBySchoolIdAndTemplateIdAndDeleted(Long schoolId, Long templateId, Integer deleted);

    boolean existsBySchoolIdAndTemplateIdAndDeletedAndIdNot(
            Long schoolId, Long templateId, Integer deleted, Long id);

    boolean existsByTemplateIdAndDeleted(Long templateId, Integer deleted);

    boolean existsByTemplateIdAndDeletedAndIdNot(Long templateId, Integer deleted, Long id);
}
