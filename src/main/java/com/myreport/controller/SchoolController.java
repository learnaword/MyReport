package com.myreport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

/**
 * 学校管理已下线：列表/导入接口返回失败；旧导入入口重定向到数据管理。
 */
@Controller
@RequestMapping("/school")
public class SchoolController {

    private static final String OFFLINE_MESSAGE = "学校管理已下线";

    @GetMapping("/import")
    public RedirectView importPage() {
        return new RedirectView("/admin/index.html#data");
    }

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return offline();
    }

    @PostMapping("/importExcel")
    @ResponseBody
    public Map<String, Object> importExcel(@RequestParam(value = "file", required = false) MultipartFile file) {
        return offline();
    }

    @GetMapping("/importProgress")
    @ResponseBody
    public Map<String, Object> importProgress(@RequestParam(value = "taskId", required = false) String taskId) {
        return offline();
    }

    private static Map<String, Object> offline() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", -1);
        result.put("message", OFFLINE_MESSAGE);
        return result;
    }
}
