package com.myreport.repository;

import com.myreport.entity.ReportTemplateNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 报告模版节点仓储。
 */
public interface ReportTemplateNodeRepository extends JpaRepository<ReportTemplateNode, Long> {

    Optional<ReportTemplateNode> findByIdAndDeleted(Long id, Integer deleted);

    List<ReportTemplateNode> findByTemplateIdAndDeletedOrderBySortOrderAsc(Long templateId, Integer deleted);

    List<ReportTemplateNode> findByTemplateIdAndParentIdAndDeletedOrderBySortOrderAsc(
            Long templateId, Long parentId, Integer deleted);

    List<ReportTemplateNode> findByTemplateIdAndParentIdIsNullAndDeletedOrderBySortOrderAsc(
            Long templateId, Integer deleted);
}
