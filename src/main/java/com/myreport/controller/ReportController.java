package com.myreport.controller;

import com.alibaba.fastjson.JSONObject;
import com.myreport.util.word.SpireReportUtil;
import com.myreport.vo.CreateReportVO;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告相关接口。
 */
@RestController
@RequestMapping("/report")
public class ReportController {

    private static final Logger logger = Logger.getLogger(ReportController.class);

    /**
     * 创建报告。
     */
    @PostMapping("/createReport")
    public Map<String, Object> createReport(@RequestBody CreateReportVO createReportVO) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            if (createReportVO == null || createReportVO.getReportId() == null) {
                result.put("code", -1);
                result.put("message", "reportId不能为空");
                return result;
            }

            JSONObject overallSetting = createReportVO.getOverallSetting() != null
                    ? createReportVO.getOverallSetting()
                    : new JSONObject();

            SpireReportUtil.createReport(
                    createReportVO.getReportJsonArr(),
                    overallSetting,
                    createReportVO
            );

            result.put("code", 0);
            result.put("message", "报告生成任务已提交");
            result.put("reportId", createReportVO.getReportId());
        } catch (Exception e) {
            logger.error("createReport failed", e);
            result.put("code", -1);
            result.put("message", "创建报告失败：" + e.getMessage());
        }
        return result;
    }
}
