package com.myreport.service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Word 生成终态回写 simple_report_run（供 SpireReportUtil 静态调用）。
 */
@Component
public class SimpleReportGenerateSync {

    private static final Logger logger = Logger.getLogger(SimpleReportGenerateSync.class);

    private static volatile SimpleReportGenerateSync INSTANCE;

    private final SimpleReportService simpleReportService;

    public SimpleReportGenerateSync(SimpleReportService simpleReportService) {
        this.simpleReportService = simpleReportService;
    }

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    public static void onSuccess(Integer reportId, String filePath) {
        SimpleReportGenerateSync sync = INSTANCE;
        if (sync == null || reportId == null) {
            return;
        }
        try {
            sync.simpleReportService.onGenerateSuccess(reportId, filePath);
        } catch (Exception e) {
            logger.error("simple report sync success failed, reportId=" + reportId, e);
        }
    }

    public static void onFailure(Integer reportId, String message) {
        SimpleReportGenerateSync sync = INSTANCE;
        if (sync == null || reportId == null) {
            return;
        }
        try {
            sync.simpleReportService.onGenerateFailure(reportId, message);
        } catch (Exception e) {
            logger.error("simple report sync failure failed, reportId=" + reportId, e);
        }
    }
}
