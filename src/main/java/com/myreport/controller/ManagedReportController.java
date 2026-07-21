package com.myreport.controller;

import com.myreport.entity.ManagedReport;
import com.myreport.service.ManagedReportService;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告管理 API。
 *
 * @see docs/report_manage.md
 */
@Controller
@RequestMapping("/managed-report")
public class ManagedReportController {

    private static final Logger logger = Logger.getLogger(ManagedReportController.class);

    private final ManagedReportService managedReportService;

    public ManagedReportController(ManagedReportService managedReportService) {
        this.managedReportService = managedReportService;
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Page<ManagedReport> data = managedReportService.list(page, size);
            List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
            for (ManagedReport r : data.getContent()) {
                content.add(managedReportService.toListItem(r));
            }
            result.put("code", 0);
            result.put("message", "ok");
            result.put("content", content);
            result.put("page", data.getNumber());
            result.put("size", data.getSize());
            result.put("totalElements", data.getTotalElements());
            result.put("totalPages", data.getTotalPages());
        } catch (Exception e) {
            logger.error("managed-report list failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    @GetMapping("/detail")
    @ResponseBody
    public Map<String, Object> detail(@RequestParam("id") Long id) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> detail = managedReportService.detail(id);
            result.put("code", 0);
            result.put("message", "ok");
            result.putAll(detail);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("managed-report detail failed", e);
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
            Long schoolId = toLong(body.get("schoolId"));
            Long templateId = toLong(body.get("templateId"));
            Long id = managedReportService.create(name, schoolId, templateId);
            result.put("code", 0);
            result.put("message", "创建成功");
            result.put("id", id);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("managed-report create failed", e);
            result.put("code", -1);
            result.put("message", "创建失败");
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
            Long schoolId = body.containsKey("schoolId") ? toLong(body.get("schoolId")) : null;
            Long templateId = body.containsKey("templateId") ? toLong(body.get("templateId")) : null;
            managedReportService.update(id, name, schoolId, templateId);
            result.put("code", 0);
            result.put("message", "更新成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("managed-report update failed", e);
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
            managedReportService.delete(id);
            result.put("code", 0);
            result.put("message", "删除成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("managed-report delete failed", e);
            result.put("code", -1);
            result.put("message", "删除失败");
        }
        return result;
    }

    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generate(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            managedReportService.generate(id);
            result.put("code", 0);
            result.put("message", "报告生成任务已提交");
            result.put("reportId", id);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("managed-report generate failed", e);
            result.put("code", -1);
            result.put("message", "提交生成失败");
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
            String s = String.valueOf(v).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
                return null;
            }
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
