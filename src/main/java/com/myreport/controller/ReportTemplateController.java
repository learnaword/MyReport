package com.myreport.controller;

import com.myreport.entity.ReportTemplate;
import com.myreport.service.GradEmploymentStatFieldCatalog;
import com.myreport.service.ReportTemplateService;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告模版配置 API。
 *
 * @see docs/report_template_config/API设计.md
 */
@Controller
@RequestMapping("/report-template")
public class ReportTemplateController {

    private static final Logger logger = Logger.getLogger(ReportTemplateController.class);

    private final ReportTemplateService reportTemplateService;

    public ReportTemplateController(ReportTemplateService reportTemplateService) {
        this.reportTemplateService = reportTemplateService;
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Page<ReportTemplate> data = reportTemplateService.list(page, size, status, keyword);
            result.put("code", 0);
            result.put("message", "ok");
            result.put("content", data.getContent());
            result.put("page", data.getNumber());
            result.put("size", data.getSize());
            result.put("totalElements", data.getTotalElements());
            result.put("totalPages", data.getTotalPages());
        } catch (Exception e) {
            logger.error("report-template list failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
            String description = body.containsKey("description") && body.get("description") != null
                    ? String.valueOf(body.get("description")) : null;
            Integer status = toInteger(body.get("status"));
            Long id = reportTemplateService.createTemplate(name, description, status);
            result.put("code", 0);
            result.put("message", "创建成功");
            result.put("id", id);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template create failed", e);
            result.put("code", -1);
            result.put("message", "创建失败");
        }
        return result;
    }

    @GetMapping("/detail")
    @ResponseBody
    public Map<String, Object> detail(@RequestParam("id") Long id) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> detail = reportTemplateService.detail(id);
            result.put("code", 0);
            result.put("message", "ok");
            result.putAll(detail);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template detail failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    /**
     * 指标统计字段字典：label=中文，value=库列名。
     *
     * @see docs/metric_field_select/API设计.md
     */
    @GetMapping("/stat-fields")
    @ResponseBody
    public Map<String, Object> statFields() {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            result.put("code", 0);
            result.put("message", "ok");
            result.put("fields", GradEmploymentStatFieldCatalog.listOptions());
        } catch (Exception e) {
            logger.error("report-template stat-fields failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            String name = body.containsKey("name")
                    ? (body.get("name") == null ? null : String.valueOf(body.get("name")))
                    : null;
            // null = 不更新；空串 = 清空
            String description = body.containsKey("description")
                    ? (body.get("description") == null ? "" : String.valueOf(body.get("description")))
                    : null;
            Integer status = body.containsKey("status") ? toInteger(body.get("status")) : null;
            reportTemplateService.updateTemplate(id, name, description, status);
            result.put("code", 0);
            result.put("message", "更新成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template update failed", e);
            result.put("code", -1);
            result.put("message", "更新失败");
        }
        return result;
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            reportTemplateService.deleteTemplate(id);
            result.put("code", 0);
            result.put("message", "删除成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template delete failed", e);
            result.put("code", -1);
            result.put("message", "删除失败");
        }
        return result;
    }

    @PostMapping("/copy")
    @ResponseBody
    public Map<String, Object> copy(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            String name = body.containsKey("name") && body.get("name") != null
                    ? String.valueOf(body.get("name")) : null;
            Long newId = reportTemplateService.copyTemplate(id, name);
            result.put("code", 0);
            result.put("message", "复制成功");
            result.put("id", newId);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template copy failed", e);
            result.put("code", -1);
            result.put("message", "复制失败");
        }
        return result;
    }

    @PostMapping("/saveTree")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveTree(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            List<Map<String, Object>> nodes = null;
            if (body.get("nodes") instanceof List) {
                nodes = (List<Map<String, Object>>) body.get("nodes");
            }
            reportTemplateService.saveTree(id, nodes);
            result.put("code", 0);
            result.put("message", "保存成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template saveTree failed", e);
            result.put("code", -1);
            result.put("message", "保存失败");
        }
        return result;
    }

    @PostMapping("/saveStyle")
    @ResponseBody
    public Map<String, Object> saveStyle(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            reportTemplateService.saveStyle(id, body);
            result.put("code", 0);
            result.put("message", "保存成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template saveStyle failed", e);
            result.put("code", -1);
            result.put("message", "保存失败");
        }
        return result;
    }

    @PostMapping("/uploadImage")
    @ResponseBody
    public Map<String, Object> uploadImage(
            @RequestParam("id") Long id,
            @RequestParam("slot") String slot,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> uploaded = reportTemplateService.uploadImage(id, slot, file);
            result.put("code", 0);
            result.put("message", "上传成功");
            result.putAll(uploaded);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template uploadImage failed", e);
            result.put("code", -1);
            result.put("message", "上传失败");
        }
        return result;
    }

    @PostMapping("/node/create")
    @ResponseBody
    public Map<String, Object> createNode(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> created = reportTemplateService.createNode(body);
            result.put("code", 0);
            result.put("message", "创建成功");
            result.putAll(created);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template node create failed", e);
            result.put("code", -1);
            result.put("message", "创建失败");
        }
        return result;
    }

    @PostMapping("/node/update")
    @ResponseBody
    public Map<String, Object> updateNode(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            reportTemplateService.updateNode(body);
            result.put("code", 0);
            result.put("message", "更新成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template node update failed", e);
            result.put("code", -1);
            result.put("message", "更新失败");
        }
        return result;
    }

    @PostMapping("/node/delete")
    @ResponseBody
    public Map<String, Object> deleteNode(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            reportTemplateService.deleteNode(id);
            result.put("code", 0);
            result.put("message", "删除成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template node delete failed", e);
            result.put("code", -1);
            result.put("message", "删除失败");
        }
        return result;
    }

    @PostMapping("/node/move")
    @ResponseBody
    public Map<String, Object> moveNode(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> moved = reportTemplateService.moveNode(body);
            result.put("code", 0);
            result.put("message", "移动成功");
            result.putAll(moved);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("report-template node move failed", e);
            result.put("code", -1);
            result.put("message", "移动失败");
        }
        return result;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInteger(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
