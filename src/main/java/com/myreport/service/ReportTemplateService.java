package com.myreport.service;

import com.myreport.config.ReportTemplateUploadConfig;
import com.myreport.entity.ReportTemplate;
import com.myreport.entity.ReportTemplateNode;
import com.myreport.repository.ReportTemplateNodeRepository;
import com.myreport.repository.ReportTemplateRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 报告模版：CRUD、树组装、软删级联、复制、拖拽。
 */
@Service
public class ReportTemplateService {

    private static final Logger logger = Logger.getLogger(ReportTemplateService.class);

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;

    private static final String SLOT_COVER = "cover";
    private static final String SLOT_BACK = "backCover";

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateNodeRepository nodeRepository;
    private final ReportTemplateUploadConfig uploadConfig;

    public ReportTemplateService(ReportTemplateRepository templateRepository,
                                 ReportTemplateNodeRepository nodeRepository,
                                 ReportTemplateUploadConfig uploadConfig) {
        this.templateRepository = templateRepository;
        this.nodeRepository = nodeRepository;
        this.uploadConfig = uploadConfig;
    }

    public Page<ReportTemplate> list(int page, int size, Integer status, String keyword) {
        int safePage = page < 0 ? 0 : page;
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updateTime"));
        boolean hasKw = StringUtils.isNotBlank(keyword);
        boolean hasStatus = status != null;
        if (hasStatus && hasKw) {
            return templateRepository.findByDeletedAndStatusAndNameContaining(
                    NOT_DELETED, status, keyword.trim(), pageable);
        }
        if (hasStatus) {
            return templateRepository.findByDeletedAndStatus(NOT_DELETED, status, pageable);
        }
        if (hasKw) {
            return templateRepository.findByDeletedAndNameContaining(NOT_DELETED, keyword.trim(), pageable);
        }
        return templateRepository.findByDeleted(NOT_DELETED, pageable);
    }

    @Transactional
    public Long createTemplate(String name, String description, Integer status) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("模版名称不能为空");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("模版名称不能超过128字");
        }
        if (description != null && description.length() > 512) {
            throw new IllegalArgumentException("模版说明不能超过512字");
        }
        ReportTemplate t = new ReportTemplate();
        t.setName(name.trim());
        t.setDescription(description);
        t.setStatus(status == null ? 1 : status);
        t.setDeleted(NOT_DELETED);
        templateRepository.save(t);
        return t.getId();
    }

    public Map<String, Object> detail(Long id) {
        ReportTemplate t = requireTemplate(id);
        List<ReportTemplateNode> nodes = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(id, NOT_DELETED);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("templateId", t.getId());
        result.put("name", t.getName());
        result.put("description", t.getDescription() == null ? "" : t.getDescription());
        result.put("status", t.getStatus());
        result.put("coverImage", t.getCoverImage());
        result.put("backCoverImage", t.getBackCoverImage());
        result.put("createTime", t.getCreateTime());
        result.put("updateTime", t.getUpdateTime());
        result.put("nodes", buildTree(nodes));
        return result;
    }

    /**
     * 保存样式：只更新封面/底图，不碰节点树。
     * body 中未出现的 key 保持原值；null 或空串表示清除。
     */
    @Transactional
    public void saveStyle(Long id, Map<String, Object> body) {
        ReportTemplate t = requireTemplate(id);
        boolean hasCover = body != null && body.containsKey("coverImage");
        boolean hasBack = body != null && body.containsKey("backCoverImage");
        if (!hasCover && !hasBack) {
            throw new IllegalArgumentException("请至少提交一个样式字段");
        }
        if (hasCover) {
            String next = normalizeImageRef(body.get("coverImage"), "封面图");
            replaceImageRef(t.getCoverImage(), next);
            t.setCoverImage(next);
        }
        if (hasBack) {
            String next = normalizeImageRef(body.get("backCoverImage"), "底图");
            replaceImageRef(t.getBackCoverImage(), next);
            t.setBackCoverImage(next);
        }
        templateRepository.save(t);
    }

    /**
     * 上传封面或底图；成功即写库并返回当前外观。
     */
    @Transactional
    public Map<String, Object> uploadImage(Long templateId, String slot, MultipartFile file) {
        ReportTemplate t = requireTemplate(templateId);
        if (!SLOT_COVER.equals(slot) && !SLOT_BACK.equals(slot)) {
            throw new IllegalArgumentException("slot 必须为 cover 或 backCover");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("图片不能超过 2MB");
        }
        String ext = resolveImageExt(file.getOriginalFilename());
        if (ext == null) {
            throw new IllegalArgumentException("图片格式仅支持 jpg/png");
        }

        String fileName = (SLOT_COVER.equals(slot) ? "cover" : "back") + "." + ext;
        File dir = new File(uploadConfig.getTemplateImageRoot(), String.valueOf(templateId));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalArgumentException("创建上传目录失败");
        }
        deleteSlotFiles(dir, SLOT_COVER.equals(slot) ? "cover" : "back");

        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            logger.error("upload image failed, templateId=" + templateId, e);
            throw new IllegalArgumentException("上传失败，请重试");
        }

        String url = ReportTemplateUploadConfig.URL_PREFIX + templateId + "/" + fileName;
        String old = SLOT_COVER.equals(slot) ? t.getCoverImage() : t.getBackCoverImage();
        if (old != null && !old.equals(url)) {
            deleteManagedFileQuietly(old);
        }
        if (SLOT_COVER.equals(slot)) {
            t.setCoverImage(url);
        } else {
            t.setBackCoverImage(url);
        }
        templateRepository.save(t);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("slot", slot);
        result.put("url", url);
        result.put("coverImage", t.getCoverImage());
        result.put("backCoverImage", t.getBackCoverImage());
        return result;
    }

    private static String normalizeImageRef(Object raw, String label) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        if (s.length() > 1024) {
            throw new IllegalArgumentException(label + "不能超过1024字");
        }
        return s;
    }

    @Transactional
    public void updateTemplate(Long id, String name, String description, Integer status) {
        ReportTemplate t = requireTemplate(id);
        if (name != null) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("模版名称不能为空");
            }
            if (name.length() > 128) {
                throw new IllegalArgumentException("模版名称不能超过128字");
            }
            t.setName(name.trim());
        }
        if (description != null) {
            if (description.length() > 512) {
                throw new IllegalArgumentException("模版说明不能超过512字");
            }
            t.setDescription(description);
        }
        if (status != null) {
            if (status != 0 && status != 1) {
                throw new IllegalArgumentException("status 只能为 0 或 1");
            }
            t.setStatus(status);
        }
        templateRepository.save(t);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ReportTemplate t = templateRepository.findById(id).orElse(null);
        if (t == null || Integer.valueOf(DELETED).equals(t.getDeleted())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        t.setDeleted(DELETED);
        t.setDeleteTime(now);
        templateRepository.save(t);

        List<ReportTemplateNode> nodes = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(id, NOT_DELETED);
        for (ReportTemplateNode n : nodes) {
            softDeleteNodeEntity(n, now);
        }
        if (!nodes.isEmpty()) {
            nodeRepository.saveAll(nodes);
        }
    }

    @Transactional
    public Long copyTemplate(Long id, String newName) {
        ReportTemplate src = requireTemplate(id);
        String name = StringUtils.isNotBlank(newName)
                ? newName.trim()
                : src.getName() + "（副本）";
        if (name.length() > 128) {
            throw new IllegalArgumentException("模版名称不能超过128字");
        }

        ReportTemplate copy = new ReportTemplate();
        copy.setName(name);
        copy.setDescription(src.getDescription());
        copy.setStatus(src.getStatus());
        copy.setDeleted(NOT_DELETED);
        templateRepository.save(copy);
        copy.setCoverImage(copyManagedImage(src.getCoverImage(), src.getId(), copy.getId(), "cover"));
        copy.setBackCoverImage(copyManagedImage(src.getBackCoverImage(), src.getId(), copy.getId(), "back"));
        templateRepository.save(copy);

        List<ReportTemplateNode> srcNodes = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(id, NOT_DELETED);
        Map<Long, Long> idMap = new HashMap<Long, Long>();

        // 按层级大致顺序：先保存全部再二次写 parent（两遍：先无 parent，再填 parent）
        List<ReportTemplateNode> created = new ArrayList<ReportTemplateNode>();
        for (ReportTemplateNode sn : srcNodes) {
            ReportTemplateNode nn = new ReportTemplateNode();
            nn.setTemplateId(copy.getId());
            nn.setParentId(null);
            nn.setNodeType(sn.getNodeType());
            nn.setLevel(sn.getLevel());
            nn.setName(sn.getName());
            nn.setIntro(sn.getIntro());
            nn.setSortOrder(sn.getSortOrder());
            nn.setStatField(sn.getStatField());
            nn.setValueFormat(sn.getValueFormat());
            nn.setDisplayType(sn.getDisplayType());
            nn.setChartStyle(sn.getChartStyle());
            nn.setDeleted(NOT_DELETED);
            nodeRepository.save(nn);
            idMap.put(sn.getId(), nn.getId());
            created.add(nn);
        }
        for (int i = 0; i < srcNodes.size(); i++) {
            ReportTemplateNode sn = srcNodes.get(i);
            ReportTemplateNode nn = created.get(i);
            if (sn.getParentId() != null) {
                Long newParent = idMap.get(sn.getParentId());
                nn.setParentId(newParent);
            }
        }
        nodeRepository.saveAll(created);
        return copy.getId();
    }

    /**
     * 整树保存：软删原节点后按提交树重建（供编辑结构弹窗一次保存）。
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void saveTree(Long templateId, List<Map<String, Object>> nodes) {
        ReportTemplate t = requireTemplate(templateId);
        if (nodes == null) {
            nodes = new ArrayList<Map<String, Object>>();
        }
        LocalDateTime now = LocalDateTime.now();
        List<ReportTemplateNode> old = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(templateId, NOT_DELETED);
        for (ReportTemplateNode n : old) {
            softDeleteNodeEntity(n, now);
        }
        if (!old.isEmpty()) {
            nodeRepository.saveAll(old);
        }
        insertTreeNodes(templateId, null, 1, nodes);
        t.setUpdateTime(LocalDateTime.now());
        templateRepository.save(t);
    }

    @SuppressWarnings("unchecked")
    private void insertTreeNodes(Long templateId, Long parentId, int level,
                                 List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> raw = nodes.get(i);
            if (raw == null) {
                throw new IllegalArgumentException("节点不能为空");
            }
            String nodeType = str(raw.get("nodeType"));
            if (!ReportTemplateNode.TYPE_TITLE.equals(nodeType)
                    && !ReportTemplateNode.TYPE_METRIC.equals(nodeType)) {
                throw new IllegalArgumentException("nodeType 必须为 TITLE 或 METRIC");
            }
            if (parentId == null && !ReportTemplateNode.TYPE_TITLE.equals(nodeType)) {
                throw new IllegalArgumentException("根节点只能是标题");
            }
            if (ReportTemplateNode.TYPE_METRIC.equals(nodeType) && parentId == null) {
                throw new IllegalArgumentException("指标不能作为根节点");
            }

            String name = str(raw.get("name"));
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("名称不能为空");
            }
            if (name.length() > 256) {
                throw new IllegalArgumentException("名称不能超过256字");
            }

            ReportTemplateNode node = new ReportTemplateNode();
            node.setTemplateId(templateId);
            node.setParentId(parentId);
            node.setNodeType(nodeType);
            node.setLevel(level);
            node.setName(name.trim());
            node.setSortOrder(i);
            node.setDeleted(NOT_DELETED);

            List<Map<String, Object>> children = null;
            if (raw.get("children") instanceof List) {
                children = (List<Map<String, Object>>) raw.get("children");
            }

            if (ReportTemplateNode.TYPE_TITLE.equals(nodeType)) {
                String intro = str(raw.get("intro"));
                if (intro != null && intro.length() > 2000) {
                    throw new IllegalArgumentException("介绍不能超过2000字");
                }
                node.setIntro(intro);
                clearMetricFields(node);
                nodeRepository.save(node);
                insertTreeNodes(templateId, node.getId(), level + 1, children);
            } else {
                if (children != null && !children.isEmpty()) {
                    throw new IllegalArgumentException("指标不能包含子节点");
                }
                applyMetricFields(node, raw, true);
                node.setIntro(null);
                nodeRepository.save(node);
            }
        }
    }

    @Transactional
    public Map<String, Object> createNode(Map<String, Object> body) {
        Long templateId = toLong(body.get("templateId"));
        if (templateId == null) {
            throw new IllegalArgumentException("templateId 不能为空");
        }
        requireTemplate(templateId);

        String nodeType = str(body.get("nodeType"));
        if (!ReportTemplateNode.TYPE_TITLE.equals(nodeType)
                && !ReportTemplateNode.TYPE_METRIC.equals(nodeType)) {
            throw new IllegalArgumentException("nodeType 必须为 TITLE 或 METRIC");
        }
        String name = str(body.get("name"));
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (name.length() > 256) {
            throw new IllegalArgumentException("名称不能超过256字");
        }

        Long parentId = toLong(body.get("parentId"));
        ReportTemplateNode parent = null;
        int level;
        if (parentId == null) {
            if (!ReportTemplateNode.TYPE_TITLE.equals(nodeType)) {
                throw new IllegalArgumentException("根节点只能是标题");
            }
            level = 1;
        } else {
            parent = requireNode(parentId);
            if (!parent.getTemplateId().equals(templateId)) {
                throw new IllegalArgumentException("父节点与模版不匹配");
            }
            if (!ReportTemplateNode.TYPE_TITLE.equals(parent.getNodeType())) {
                throw new IllegalArgumentException("只能在标题下添加子节点");
            }
            level = parent.getLevel() + 1;
        }

        ReportTemplateNode node = new ReportTemplateNode();
        node.setTemplateId(templateId);
        node.setParentId(parentId);
        node.setNodeType(nodeType);
        node.setLevel(level);
        node.setName(name.trim());
        node.setDeleted(NOT_DELETED);

        if (ReportTemplateNode.TYPE_TITLE.equals(nodeType)) {
            String intro = str(body.get("intro"));
            if (intro != null && intro.length() > 2000) {
                throw new IllegalArgumentException("介绍不能超过2000字");
            }
            node.setIntro(intro);
            clearMetricFields(node);
        } else {
            applyMetricFields(node, body, true);
            node.setIntro(null);
        }

        Integer sortOrder = toInt(body.get("sortOrder"));
        if (sortOrder == null) {
            List<ReportTemplateNode> siblings = listSiblings(templateId, parentId);
            sortOrder = siblings.isEmpty() ? 0 : siblings.get(siblings.size() - 1).getSortOrder() + 1;
        }
        node.setSortOrder(sortOrder);
        nodeRepository.save(node);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", node.getId());
        result.put("level", node.getLevel());
        return result;
    }

    @Transactional
    public void updateNode(Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        ReportTemplateNode node = requireNode(id);

        if (body.containsKey("name")) {
            String name = str(body.get("name"));
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("名称不能为空");
            }
            if (name.length() > 256) {
                throw new IllegalArgumentException("名称不能超过256字");
            }
            node.setName(name.trim());
        }

        if (ReportTemplateNode.TYPE_TITLE.equals(node.getNodeType())) {
            if (body.containsKey("intro")) {
                String intro = str(body.get("intro"));
                if (intro != null && intro.length() > 2000) {
                    throw new IllegalArgumentException("介绍不能超过2000字");
                }
                node.setIntro(intro);
            }
        } else {
            applyMetricFields(node, body, false);
        }
        nodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long id) {
        ReportTemplateNode node = nodeRepository.findById(id).orElse(null);
        if (node == null || Integer.valueOf(DELETED).equals(node.getDeleted())) {
            return;
        }
        List<ReportTemplateNode> all = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(node.getTemplateId(), NOT_DELETED);
        Map<Long, List<ReportTemplateNode>> childrenMap = childrenMap(all);
        List<ReportTemplateNode> toDelete = new ArrayList<ReportTemplateNode>();
        collectSubtree(node, childrenMap, toDelete);
        LocalDateTime now = LocalDateTime.now();
        for (ReportTemplateNode n : toDelete) {
            softDeleteNodeEntity(n, now);
        }
        nodeRepository.saveAll(toDelete);

        // 压缩旧父下剩余兄弟顺序
        compressSiblings(node.getTemplateId(), node.getParentId());
    }

    @Transactional
    public Map<String, Object> moveNode(Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        ReportTemplateNode node = requireNode(id);
        Long oldParentId = node.getParentId();

        Long newParentId;
        if (body.containsKey("parentId")) {
            Object raw = body.get("parentId");
            newParentId = raw == null || "".equals(raw) ? null : toLong(raw);
        } else {
            newParentId = oldParentId;
        }

        @SuppressWarnings("unchecked")
        List<Object> orderedRaw = body.get("orderedSiblingIds") instanceof List
                ? (List<Object>) body.get("orderedSiblingIds")
                : null;
        if (orderedRaw == null || orderedRaw.isEmpty()) {
            throw new IllegalArgumentException("orderedSiblingIds 不能为空");
        }
        List<Long> orderedIds = new ArrayList<Long>();
        for (Object o : orderedRaw) {
            Long sid = toLong(o);
            if (sid == null) {
                throw new IllegalArgumentException("orderedSiblingIds 含非法 id");
            }
            orderedIds.add(sid);
        }
        if (!orderedIds.contains(id)) {
            throw new IllegalArgumentException("orderedSiblingIds 必须包含被移动节点");
        }

        ReportTemplateNode newParent = null;
        int newLevel;
        if (newParentId == null) {
            if (!ReportTemplateNode.TYPE_TITLE.equals(node.getNodeType())) {
                throw new IllegalArgumentException("指标不能作为根节点");
            }
            newLevel = 1;
        } else {
            newParent = requireNode(newParentId);
            if (!newParent.getTemplateId().equals(node.getTemplateId())) {
                throw new IllegalArgumentException("不能跨模版移动");
            }
            if (!ReportTemplateNode.TYPE_TITLE.equals(newParent.getNodeType())) {
                throw new IllegalArgumentException("新父节点必须是标题");
            }
            if (isDescendant(node.getId(), newParentId, node.getTemplateId())) {
                throw new IllegalArgumentException("不能将节点移动到自身子树下");
            }
            newLevel = newParent.getLevel() + 1;
        }

        // 校验 siblings 都属于新父、同模版、未删，且集合完整
        List<ReportTemplateNode> expectedSiblings = listSiblings(node.getTemplateId(), newParentId);
        Set<Long> expectedIds = new HashSet<Long>();
        for (ReportTemplateNode s : expectedSiblings) {
            if (!s.getId().equals(id)) {
                expectedIds.add(s.getId());
            }
        }
        // 若换父，旧位置的 id 不在 expected；新父下应 = expected + id
        Set<Long> orderedSet = new HashSet<Long>(orderedIds);
        if (orderedSet.size() != orderedIds.size()) {
            throw new IllegalArgumentException("orderedSiblingIds 不能重复");
        }
        Set<Long> expectFull = new HashSet<Long>(expectedIds);
        expectFull.add(id);
        if (!orderedSet.equals(expectFull)) {
            throw new IllegalArgumentException("orderedSiblingIds 必须恰好覆盖新父下全部子节点");
        }

        node.setParentId(newParentId);
        int delta = newLevel - node.getLevel();
        node.setLevel(newLevel);

        List<ReportTemplateNode> all = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(node.getTemplateId(), NOT_DELETED);
        Map<Long, List<ReportTemplateNode>> childrenMap = childrenMap(all);
        if (delta != 0) {
            shiftSubtreeLevel(node, childrenMap, delta);
        }

        Map<Long, ReportTemplateNode> byId = new HashMap<Long, ReportTemplateNode>();
        for (ReportTemplateNode n : all) {
            byId.put(n.getId(), n);
        }
        byId.put(node.getId(), node);

        List<ReportTemplateNode> toSave = new ArrayList<ReportTemplateNode>();
        toSave.add(node);
        collectSubtreeExceptRoot(node, childrenMap, toSave);

        for (int i = 0; i < orderedIds.size(); i++) {
            ReportTemplateNode sib = byId.get(orderedIds.get(i));
            if (sib == null) {
                throw new IllegalArgumentException("兄弟节点不存在: " + orderedIds.get(i));
            }
            sib.setParentId(newParentId);
            sib.setSortOrder(i);
            if (!toSave.contains(sib)) {
                toSave.add(sib);
            }
        }
        nodeRepository.saveAll(toSave);

        if (oldParentId == null ? newParentId != null : !oldParentId.equals(newParentId)) {
            compressSiblings(node.getTemplateId(), oldParentId);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("level", node.getLevel());
        return result;
    }

    // ——— helpers ———

    private ReportTemplate requireTemplate(Long id) {
        ReportTemplate t = templateRepository.findByIdAndDeleted(id, NOT_DELETED).orElse(null);
        if (t == null) {
            throw new IllegalArgumentException("模版不存在或已删除");
        }
        return t;
    }

    private ReportTemplateNode requireNode(Long id) {
        ReportTemplateNode n = nodeRepository.findByIdAndDeleted(id, NOT_DELETED).orElse(null);
        if (n == null) {
            throw new IllegalArgumentException("节点不存在或已删除");
        }
        return n;
    }

    private List<ReportTemplateNode> listSiblings(Long templateId, Long parentId) {
        if (parentId == null) {
            return nodeRepository.findByTemplateIdAndParentIdIsNullAndDeletedOrderBySortOrderAsc(
                    templateId, NOT_DELETED);
        }
        return nodeRepository.findByTemplateIdAndParentIdAndDeletedOrderBySortOrderAsc(
                templateId, parentId, NOT_DELETED);
    }

    private void compressSiblings(Long templateId, Long parentId) {
        List<ReportTemplateNode> siblings = listSiblings(templateId, parentId);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i);
        }
        if (!siblings.isEmpty()) {
            nodeRepository.saveAll(siblings);
        }
    }

    private void softDeleteNodeEntity(ReportTemplateNode n, LocalDateTime now) {
        n.setDeleted(DELETED);
        n.setDeleteTime(now);
    }

    private void clearMetricFields(ReportTemplateNode node) {
        node.setStatField(null);
        node.setValueFormat(null);
        node.setDisplayType(null);
        node.setChartStyle(null);
    }

    private void applyMetricFields(ReportTemplateNode node, Map<String, Object> body, boolean create) {
        String statField = body.containsKey("statField") || create ? str(body.get("statField")) : node.getStatField();
        String valueFormat = body.containsKey("valueFormat") || create ? str(body.get("valueFormat")) : node.getValueFormat();
        String displayType = body.containsKey("displayType") || create ? str(body.get("displayType")) : node.getDisplayType();
        String chartStyle;
        if (body.containsKey("chartStyle") || create) {
            chartStyle = str(body.get("chartStyle"));
        } else {
            chartStyle = node.getChartStyle();
        }

        if (StringUtils.isBlank(statField)) {
            throw new IllegalArgumentException("统计字段不能为空");
        }
        if (statField.length() > 128) {
            throw new IllegalArgumentException("统计字段不能超过128字");
        }
        if (!ReportTemplateNode.FORMAT_PERCENT.equals(valueFormat)
                && !ReportTemplateNode.FORMAT_COUNT.equals(valueFormat)) {
            throw new IllegalArgumentException("valueFormat 必须为 PERCENT 或 COUNT");
        }
        if (!ReportTemplateNode.DISPLAY_TABLE.equals(displayType)
                && !ReportTemplateNode.DISPLAY_CHART.equals(displayType)) {
            throw new IllegalArgumentException("displayType 必须为 TABLE 或 CHART");
        }
        if (ReportTemplateNode.DISPLAY_CHART.equals(displayType)) {
            if (!ReportTemplateNode.CHART_BAR.equals(chartStyle)
                    && !ReportTemplateNode.CHART_PIE.equals(chartStyle)
                    && !ReportTemplateNode.CHART_LINE.equals(chartStyle)) {
                throw new IllegalArgumentException("chartStyle 必须为 BAR / PIE / LINE");
            }
        } else {
            chartStyle = null;
        }
        node.setStatField(statField.trim());
        node.setValueFormat(valueFormat);
        node.setDisplayType(displayType);
        node.setChartStyle(chartStyle);
    }

    private List<Map<String, Object>> buildTree(List<ReportTemplateNode> nodes) {
        Map<Long, List<ReportTemplateNode>> childrenMap = childrenMap(nodes);
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        for (ReportTemplateNode n : nodes) {
            if (n.getParentId() == null) {
                roots.add(toTreeNode(n, childrenMap));
            }
        }
        return roots;
    }

    private Map<String, Object> toTreeNode(ReportTemplateNode n, Map<Long, List<ReportTemplateNode>> childrenMap) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", n.getId());
        m.put("nodeType", n.getNodeType());
        m.put("level", n.getLevel());
        m.put("name", n.getName());
        if (ReportTemplateNode.TYPE_TITLE.equals(n.getNodeType())) {
            m.put("intro", n.getIntro() == null ? "" : n.getIntro());
        } else {
            m.put("statField", n.getStatField());
            m.put("valueFormat", n.getValueFormat());
            m.put("displayType", n.getDisplayType());
            m.put("chartStyle", n.getChartStyle());
        }
        List<ReportTemplateNode> children = childrenMap.get(n.getId());
        List<Map<String, Object>> childMaps = new ArrayList<Map<String, Object>>();
        if (children != null) {
            for (ReportTemplateNode c : children) {
                childMaps.add(toTreeNode(c, childrenMap));
            }
        }
        m.put("children", childMaps);
        return m;
    }

    private Map<Long, List<ReportTemplateNode>> childrenMap(List<ReportTemplateNode> nodes) {
        Map<Long, List<ReportTemplateNode>> map = new HashMap<Long, List<ReportTemplateNode>>();
        for (ReportTemplateNode n : nodes) {
            if (n.getParentId() == null) {
                continue;
            }
            List<ReportTemplateNode> list = map.get(n.getParentId());
            if (list == null) {
                list = new ArrayList<ReportTemplateNode>();
                map.put(n.getParentId(), list);
            }
            list.add(n);
        }
        for (List<ReportTemplateNode> list : map.values()) {
            Collections.sort(list, (a, b) -> Integer.compare(
                    a.getSortOrder() == null ? 0 : a.getSortOrder(),
                    b.getSortOrder() == null ? 0 : b.getSortOrder()));
        }
        return map;
    }

    private void collectSubtree(ReportTemplateNode root,
                                Map<Long, List<ReportTemplateNode>> childrenMap,
                                List<ReportTemplateNode> out) {
        out.add(root);
        collectSubtreeExceptRoot(root, childrenMap, out);
    }

    private void collectSubtreeExceptRoot(ReportTemplateNode root,
                                          Map<Long, List<ReportTemplateNode>> childrenMap,
                                          List<ReportTemplateNode> out) {
        List<ReportTemplateNode> children = childrenMap.get(root.getId());
        if (children == null) {
            return;
        }
        for (ReportTemplateNode c : children) {
            out.add(c);
            collectSubtreeExceptRoot(c, childrenMap, out);
        }
    }

    private void shiftSubtreeLevel(ReportTemplateNode root,
                                   Map<Long, List<ReportTemplateNode>> childrenMap,
                                   int delta) {
        List<ReportTemplateNode> children = childrenMap.get(root.getId());
        if (children == null) {
            return;
        }
        for (ReportTemplateNode c : children) {
            c.setLevel(c.getLevel() + delta);
            shiftSubtreeLevel(c, childrenMap, delta);
        }
    }

    private boolean isDescendant(Long ancestorId, Long candidateId, Long templateId) {
        if (ancestorId.equals(candidateId)) {
            return true;
        }
        List<ReportTemplateNode> all = nodeRepository
                .findByTemplateIdAndDeletedOrderBySortOrderAsc(templateId, NOT_DELETED);
        Map<Long, List<ReportTemplateNode>> childrenMap = childrenMap(all);
        return containsInSubtree(ancestorId, candidateId, childrenMap);
    }

    private boolean containsInSubtree(Long rootId, Long targetId,
                                      Map<Long, List<ReportTemplateNode>> childrenMap) {
        List<ReportTemplateNode> children = childrenMap.get(rootId);
        if (children == null) {
            return false;
        }
        for (ReportTemplateNode c : children) {
            if (c.getId().equals(targetId)) {
                return true;
            }
            if (containsInSubtree(c.getId(), targetId, childrenMap)) {
                return true;
            }
        }
        return false;
    }

    private void replaceImageRef(String oldRef, String newRef) {
        if (oldRef == null || oldRef.equals(newRef)) {
            return;
        }
        deleteManagedFileQuietly(oldRef);
    }

    private void deleteManagedFileQuietly(String url) {
        File file = resolveManagedFile(url);
        if (file == null || !file.isFile()) {
            return;
        }
        if (!file.delete()) {
            logger.warn("failed to delete managed image: " + file.getAbsolutePath());
        }
    }

    private File resolveManagedFile(String url) {
        if (!isManagedUrl(url)) {
            return null;
        }
        String relative = url.substring(ReportTemplateUploadConfig.URL_PREFIX.length());
        if (relative.contains("..") || relative.startsWith("/") || relative.startsWith("\\")) {
            return null;
        }
        return new File(uploadConfig.getTemplateImageRoot(), relative);
    }

    private static boolean isManagedUrl(String url) {
        return StringUtils.isNotBlank(url) && url.startsWith(ReportTemplateUploadConfig.URL_PREFIX);
    }

    private static String resolveImageExt(String filename) {
        if (filename == null) {
            return null;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "jpg";
        }
        if (lower.endsWith(".png")) {
            return "png";
        }
        return null;
    }

    private void deleteSlotFiles(File dir, String slotPrefix) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (name.startsWith(slotPrefix + ".") && f.isFile()) {
                if (!f.delete()) {
                    logger.warn("failed to delete slot file: " + f.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 复制模版时：本服务托管图物理复制到新目录；外链直接复用字符串。
     */
    private String copyManagedImage(String srcUrl, Long srcTemplateId, Long destTemplateId, String slotPrefix) {
        if (StringUtils.isBlank(srcUrl)) {
            return null;
        }
        if (!isManagedUrl(srcUrl)) {
            return srcUrl;
        }
        File src = resolveManagedFile(srcUrl);
        if (src == null || !src.isFile()) {
            return srcUrl;
        }
        String ext = resolveImageExt(src.getName());
        if (ext == null) {
            return srcUrl;
        }
        File destDir = new File(uploadConfig.getTemplateImageRoot(), String.valueOf(destTemplateId));
        if (!destDir.exists() && !destDir.mkdirs()) {
            logger.warn("mkdir failed for copy image, templateId=" + destTemplateId);
            return srcUrl;
        }
        String fileName = slotPrefix + "." + ext;
        File dest = new File(destDir, fileName);
        try {
            Files.copy(src.toPath(), dest.toPath());
            return ReportTemplateUploadConfig.URL_PREFIX + destTemplateId + "/" + fileName;
        } catch (IOException e) {
            logger.warn("copy managed image failed: " + src.getAbsolutePath(), e);
            return srcUrl;
        }
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object v) {
        if (v == null) {
            return null;
        }
        return String.valueOf(v);
    }
}
