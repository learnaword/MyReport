package com.myreport.controller;

import com.myreport.entity.SimpleReport;
import com.myreport.entity.SimpleReportRun;
import com.myreport.service.SimpleReportService;
import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 简化报告 API。
 *
 * @see docs/ai_simple_report.md
 */
@Controller
@RequestMapping("/simple-report")
public class SimpleReportController {

    private static final Logger logger = Logger.getLogger(SimpleReportController.class);

    private final SimpleReportService simpleReportService;

    public SimpleReportController(SimpleReportService simpleReportService) {
        this.simpleReportService = simpleReportService;
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Page<SimpleReport> data = simpleReportService.list(page, size, status);
            List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
            for (SimpleReport r : data.getContent()) {
                content.add(simpleReportService.toListItem(r));
            }
            result.put("code", 0);
            result.put("message", "ok");
            result.put("content", content);
            result.put("page", data.getNumber());
            result.put("size", data.getSize());
            result.put("totalElements", data.getTotalElements());
            result.put("totalPages", data.getTotalPages());
        } catch (Exception e) {
            logger.error("simple-report list failed", e);
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
            result.put("code", 0);
            result.put("message", "ok");
            result.putAll(simpleReportService.detail(id));
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report detail failed", e);
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
            Long id = simpleReportService.create(body);
            result.put("code", 0);
            result.put("message", "创建成功");
            result.put("id", id);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report create failed", e);
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
            simpleReportService.update(body);
            result.put("code", 0);
            result.put("message", "更新成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report update failed", e);
            result.put("code", -1);
            result.put("message", "更新失败");
        }
        return result;
    }

    @PostMapping("/save-blocks")
    @ResponseBody
    public Map<String, Object> saveBlocks(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            @SuppressWarnings("unchecked")
            List<Object> blocks = body.get("blocks") instanceof List
                    ? (List<Object>) body.get("blocks")
                    : new ArrayList<Object>();
            int count = simpleReportService.saveBlocks(id, blocks);
            result.put("code", 0);
            result.put("message", "保存成功");
            result.put("blockCount", count);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report save-blocks failed", e);
            result.put("code", -1);
            result.put("message", "保存失败");
        }
        return result;
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            simpleReportService.delete(toLong(body.get("id")));
            result.put("code", 0);
            result.put("message", "删除成功");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report delete failed", e);
            result.put("code", -1);
            result.put("message", "删除失败");
        }
        return result;
    }

    @PostMapping("/plan")
    @ResponseBody
    public Map<String, Object> plan(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long id = toLong(body.get("id"));
            String userPrompt = body.get("userPrompt") == null ? null : String.valueOf(body.get("userPrompt"));
            Map<String, Object> plan = simpleReportService.plan(id, userPrompt, SimpleReportRun.TRIGGER_MANUAL);
            result.put("code", 0);
            result.put("message", "计划已生成，待确认");
            result.putAll(plan);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report plan failed", e);
            result.put("code", -1);
            result.put("message", "生成计划失败");
        }
        return result;
    }

    @PutMapping("/runs/plan")
    @ResponseBody
    public Map<String, Object> updatePlan(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Long runId = toLong(body.get("runId"));
            String planMd = body.get("planMd") == null ? null : String.valueOf(body.get("planMd"));
            simpleReportService.updatePlanMd(runId, planMd);
            result.put("code", 0);
            result.put("message", "计划文案已更新");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report update plan failed", e);
            result.put("code", -1);
            result.put("message", "更新失败");
        }
        return result;
    }

    @PostMapping("/runs/confirm")
    @ResponseBody
    public Map<String, Object> confirm(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Map<String, Object> conf = simpleReportService.confirm(toLong(body.get("runId")));
            result.put("code", 0);
            result.put("message", "报告生成任务已提交");
            result.putAll(conf);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report confirm failed", e);
            result.put("code", -1);
            result.put("message", "提交失败");
        }
        return result;
    }

    @PostMapping("/runs/cancel")
    @ResponseBody
    public Map<String, Object> cancel(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            simpleReportService.cancel(toLong(body.get("runId")));
            result.put("code", 0);
            result.put("message", "已取消");
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report cancel failed", e);
            result.put("code", -1);
            result.put("message", "取消失败");
        }
        return result;
    }

    @GetMapping("/runs/detail")
    @ResponseBody
    public Map<String, Object> runDetail(
            @RequestParam("runId") Long runId,
            @RequestParam(value = "includeSnapshot", defaultValue = "0") int includeSnapshot) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            result.put("code", 0);
            result.put("message", "ok");
            result.putAll(simpleReportService.runDetail(runId, includeSnapshot == 1));
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("simple-report run detail failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    @GetMapping("/runs/list")
    @ResponseBody
    public Map<String, Object> runList(
            @RequestParam(value = "reportId", required = false) Long reportId,
            @RequestParam(value = "runStatus", required = false) Integer runStatus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Page<SimpleReportRun> data = simpleReportService.listRuns(reportId, runStatus, page, size);
            List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
            for (SimpleReportRun r : data.getContent()) {
                content.add(simpleReportService.toRunListItem(r));
            }
            result.put("code", 0);
            result.put("message", "ok");
            result.put("content", content);
            result.put("page", data.getNumber());
            result.put("size", data.getSize());
            result.put("totalElements", data.getTotalElements());
            result.put("totalPages", data.getTotalPages());
        } catch (Exception e) {
            logger.error("simple-report run list failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    @GetMapping("/runs/download")
    public ResponseEntity<?> download(@RequestParam("runId") Long runId) {
        try {
            File file = simpleReportService.resolveDownloadFile(runId);
            Resource resource = new FileSystemResource(file);
            String encoded = encodeFileName(file.getName());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .contentLength(file.length())
                    .body(resource);
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("code", -1);
            result.put("message", e.getMessage());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
        } catch (Exception e) {
            logger.error("simple-report download failed", e);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("code", -1);
            result.put("message", "下载失败");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
        }
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeFileName(String name) throws UnsupportedEncodingException {
        return URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }
}
