package com.myreport.repository;

import com.myreport.entity.SimpleReportBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 简化报告区块仓储。
 */
public interface SimpleReportBlockRepository extends JpaRepository<SimpleReportBlock, Long> {

    List<SimpleReportBlock> findByReportIdOrderBySortOrderAscIdAsc(Long reportId);

    long countByReportId(Long reportId);

    void deleteByReportId(Long reportId);
}
