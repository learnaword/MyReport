package com.myreport.controller;

import com.alibaba.fastjson.JSONObject;
import com.myreport.entity.GradEmploymentRecord;
import com.myreport.framework.redis.ImportProgressUtil;
import com.myreport.service.GradEmploymentService;
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
 * 毕业生就业去向：列表与 Excel 导入。
 */
@Controller
@RequestMapping("/grad-employment")
public class GradEmploymentController {

    private static final Logger logger = Logger.getLogger(GradEmploymentController.class);

    private final GradEmploymentService gradEmploymentService;

    public GradEmploymentController(GradEmploymentService gradEmploymentService) {
        this.gradEmploymentService = gradEmploymentService;
    }

    @GetMapping("/import")
    public RedirectView importPage() {
        return new RedirectView("/admin/index.html#data");
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "schoolId", required = false) Long schoolId) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Page<GradEmploymentRecord> data = gradEmploymentService.list(page, size, schoolId);
            result.put("code", 0);
            result.put("message", "ok");
            result.put("content", data.getContent());
            result.put("page", data.getNumber());
            result.put("size", data.getSize());
            result.put("totalElements", data.getTotalElements());
            result.put("totalPages", data.getTotalPages());
        } catch (Exception e) {
            logger.error("grad employment list failed", e);
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
    public Map<String, Object> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "schoolId", required = false) Long schoolId) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            gradEmploymentService.validateFile(file);

            final String taskId = UUID.randomUUID().toString().replace("-", "");
            final String filename = file.getOriginalFilename();
            final File temp = File.createTempFile("grad-import-", suffixOf(filename));
            file.transferTo(temp);

            ImportProgressUtil.start(taskId, "grad-employment", "任务已提交");
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ImportProgressUtil.update(taskId, 5, 100, "开始导入…");
                        Map<String, Object> summary = gradEmploymentService.importFromExcel(
                                temp, filename, null, taskId);
                        ImportProgressUtil.success(taskId, "导入成功", summary);
                    } catch (IllegalArgumentException e) {
                        ImportProgressUtil.fail(taskId, e.getMessage());
                    } catch (Exception e) {
                        logger.error("grad employment importExcel async failed, taskId=" + taskId, e);
                        ImportProgressUtil.fail(taskId, "导入失败，请检查文件格式与表头");
                    } finally {
                        if (!temp.delete()) {
                            temp.deleteOnExit();
                        }
                    }
                }
            }, "grad-import-" + taskId);
            worker.setDaemon(true);
            worker.start();

            result.put("code", 0);
            result.put("message", "导入任务已提交");
            result.put("taskId", taskId);
        } catch (IllegalArgumentException e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            logger.error("grad employment importExcel failed", e);
            result.put("code", -1);
            result.put("message", "导入失败，请检查文件格式与表头");
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
