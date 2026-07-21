package com.myreport.repository;

import com.myreport.entity.SimpleReportRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 简化报告运行实例仓储。
 */
public interface SimpleReportRunRepository extends JpaRepository<SimpleReportRun, Long> {

    Optional<SimpleReportRun> findById(Long id);

    Optional<SimpleReportRun> findByEngineReportId(Integer engineReportId);

    Page<SimpleReportRun> findByReportId(Long reportId, Pageable pageable);

    Page<SimpleReportRun> findByRunStatus(Integer runStatus, Pageable pageable);

    Page<SimpleReportRun> findByReportIdAndRunStatus(Long reportId, Integer runStatus, Pageable pageable);

    List<SimpleReportRun> findByReportIdAndRunStatusOrderByFinishedTimeDescIdDesc(
            Long reportId, Integer runStatus, Pageable pageable);

    List<SimpleReportRun> findByReportIdAndRunStatusInOrderByCreateTimeDescIdDesc(
            Long reportId, Collection<Integer> runStatuses, Pageable pageable);

    List<SimpleReportRun> findByReportIdOrderByCreateTimeDescIdDesc(Long reportId, Pageable pageable);

    List<SimpleReportRun> findByReportIdInOrderByCreateTimeDescIdDesc(Collection<Long> reportIds);

    Page<SimpleReportRun> findAllByOrderByCreateTimeDesc(Pageable pageable);
}
