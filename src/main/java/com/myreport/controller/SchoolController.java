package com.myreport.controller;

import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.School;
import com.myreport.framework.redis.ImportProgressUtil;
import com.myreport.service.SchoolService;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 学校：列表与 Excel 导入。
 */
@Controller
@RequestMapping("/school")
public class SchoolController {

    private static final Logger logger = Logger.getLogger(SchoolController.class);

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping("/import")
    public RedirectView importPage() {
        return new RedirectView("/admin/index.html#school");
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Page<School> data = schoolService.list(page, size);
            result.put("code", 0);
            result.put("message", "ok");
            result.put("content", data.getContent());
            result.put("page", data.getNumber());
            result.put("size", data.getSize());
            result.put("totalElements", data.getTotalElements());
            result.put("totalPages", data.getTotalPages());
        } catch (Exception e) {
            logger.error("school list failed", e);
            result.put("code", -1);
            result.put("message", "查询失败");
        }
        return result;
    }

    /**
     * 异步导入：立即返回 taskId，进度见 {@link #importProgress(String)}。
     */
    @PostMapping("/importExcel")
    @ResponseBody
    public Map<String, Object> importExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            schoolService.validateFile(file);
            final String taskId = UUID.randomUUID().toString().replace("-", "");
            final String filename = file.getOriginalFilename();
            final File temp = File.createTempFile("school-import-", suffixOf(filename));
            file.transferTo(temp);

            ImportProgressUtil.start(taskId, "school", "任务已提交");
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ImportProgressUtil.update(taskId, 5, 100, "开始导入…");
                        Map<String, Object> summary = schoolService.importFromExcel(temp, filename, taskId);
                        ImportProgressUtil.success(taskId, "导入成功", summary);
                    } catch (IllegalArgumentException e) {
                        ImportProgressUtil.fail(taskId, e.getMessage());
                    } catch (Exception e) {
                        logger.error("school importExcel async failed, taskId=" + taskId, e);
                        ImportProgressUtil.fail(taskId, "导入失败，请检查文件格式");
                    } finally {
                        if (!temp.delete()) {
                            temp.deleteOnExit();
                        }
                    }
                }
            }, "school-import-" + taskId);
            worker.setDaemon(true);
            worker.start();

            result.put("code", 0);
            result.put("message", "导入任务已提交");
            result.put("taskId", taskId);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("school importExcel failed", e);
            result.put("code", -1);
            result.put("message", "导入失败，请检查文件格式");
        }
        return result;
    }

    @GetMapping("/importProgress")
    @ResponseBody
    public Map<String, Object> importProgress(@RequestParam("taskId") String taskId) {
        Map<String, Object> result = new HashMap<String, Object>();
        JSONObject state = ImportProgressUtil.get(taskId);
        if (state == null) {
            result.put("code", -1);
            result.put("message", "任务不存在或已过期");
            return result;
        }
        result.put("code", 0);
        result.put("message", "ok");
        result.putAll(state);
        return result;
    }

    private static String suffixOf(String filename) {
        if (filename == null) {
            return ".xlsx";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".xls")) {
            return ".xls";
        }
        return ".xlsx";
    }
}
