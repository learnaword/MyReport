package com.myreport.repository;

import com.myreport.entity.SimpleReportBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 简化报告区块仓储。
 */
public interface SimpleReportBlockRepository extends JpaRepository<SimpleReportBlock, Long> {

    List<SimpleReportBlock> findByReportIdOrderBySortOrderAscIdAsc(Long reportId);

    long countByReportId(Long reportId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SimpleReportBlock b where b.reportId = :reportId")
    void deleteByReportId(@Param("reportId") Long reportId);
}
