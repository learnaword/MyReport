package com.myreport.repository;

import com.myreport.entity.SimpleReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 简化报告配置仓储。
 */
public interface SimpleReportRepository extends JpaRepository<SimpleReport, Long> {

    Page<SimpleReport> findByDeleted(Integer deleted, Pageable pageable);

    Page<SimpleReport> findByDeletedAndStatus(Integer deleted, Integer status, Pageable pageable);

    Optional<SimpleReport> findByIdAndDeleted(Long id, Integer deleted);

    boolean existsByNameAndDeleted(String name, Integer deleted);

    boolean existsByNameAndDeletedAndIdNot(String name, Integer deleted, Long id);
}
