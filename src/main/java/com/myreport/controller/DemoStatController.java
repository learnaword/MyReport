package com.myreport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简化报告联调用的假数据接口（返回 labels/values）。
 * <p>
 * 本机调用示例：
 * <pre>
 * GET http://127.0.0.1:9091/demo/stat/by-college
 * GET http://127.0.0.1:9091/demo/stat/by-destination
 * GET http://127.0.0.1:9091/demo/stat/by-gender
 * GET http://127.0.0.1:9091/demo/stat/by-degree
 * GET http://127.0.0.1:9091/demo/stat/by-industry
 * GET http://127.0.0.1:9091/demo/stat/by-region
 * GET http://127.0.0.1:9091/demo/stat/employment-rate-by-year
 * </pre>
 * 本地拉数须开启 {@code myreport.simple-report.allow-loopback=true}。
 */
@Controller
@RequestMapping("/demo/stat")
public class DemoStatController {

    /**
     * 按学院人数（适合柱状图 BAR）。
     */
    @GetMapping("/by-college")
    @ResponseBody
    public Map<String, Object> byCollege(
            @RequestParam(value = "year", required = false) Integer year) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<String> labels = new ArrayList<String>(Arrays.asList(
                "计算机学院", "机械学院", "经济管理学院", "外国语学院", "艺术学院"));
        List<Number> values = new ArrayList<Number>(Arrays.asList(128, 96, 110, 64, 52));
        if (year != null && year > 0) {
            // 轻微扰动，便于观察 query 是否生效
            for (int i = 0; i < values.size(); i++) {
                values.set(i, values.get(i).intValue() + (year % 7));
            }
        }
        result.put("labels", labels);
        result.put("values", values);
        return result;
    }

    /**
     * 按毕业去向分布（适合表格 TABLE 或饼图 PIE）。
     */
    @GetMapping("/by-destination")
    @ResponseBody
    public Map<String, Object> byDestination() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("labels", Arrays.asList(
                "签就业协议", "升学", "灵活就业", "待就业", "出国出境"));
        result.put("values", Arrays.asList(210, 95, 48, 22, 15));
        return result;
    }

    /**
     * 按性别分布（适合饼图 PIE 或表格 TABLE）。
     */
    @GetMapping("/by-gender")
    @ResponseBody
    public Map<String, Object> byGender() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("labels", Arrays.asList("男", "女"));
        result.put("values", Arrays.asList(198, 192));
        return result;
    }

    /**
     * 按学历层次（适合柱状图 BAR 或饼图 PIE）。
     */
    @GetMapping("/by-degree")
    @ResponseBody
    public Map<String, Object> byDegree() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("labels", Arrays.asList("本科", "硕士", "博士"));
        result.put("values", Arrays.asList(260, 110, 20));
        return result;
    }

    /**
     * 按就业行业（适合柱状图 BAR 或表格 TABLE）。
     */
    @GetMapping("/by-industry")
    @ResponseBody
    public Map<String, Object> byIndustry() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("labels", Arrays.asList(
                "信息技术", "教育", "金融", "制造业", "医疗卫生", "其他"));
        result.put("values", Arrays.asList(85, 62, 48, 41, 28, 36));
        return result;
    }

    /**
     * 按就业地区（适合柱状图 BAR 或表格 TABLE）。
     */
    @GetMapping("/by-region")
    @ResponseBody
    public Map<String, Object> byRegion() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("labels", Arrays.asList(
                "湖北", "广东", "北京", "上海", "江苏", "其他"));
        result.put("values", Arrays.asList(120, 55, 42, 38, 35, 60));
        return result;
    }

    /**
     * 历年就业率（单位：%，适合柱状图 BAR）。
     */
    @GetMapping("/employment-rate-by-year")
    @ResponseBody
    public Map<String, Object> employmentRateByYear() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("labels", Arrays.asList("2020", "2021", "2022", "2023", "2024"));
        result.put("values", Arrays.asList(91.2, 92.5, 93.1, 94.0, 94.6));
        return result;
    }
}
