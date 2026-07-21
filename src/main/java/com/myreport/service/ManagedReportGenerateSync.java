package com.myreport.service;

import com.myreport.entity.ManagedReport;
import com.myreport.repository.ManagedReportRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Optional;

/**
 * Word 生成终态回写 managed_report（供 SpireReportUtil 静态调用）。
 */
@Component
public class ManagedReportGenerateSync {

    private static final Logger logger = Logger.getLogger(ManagedReportGenerateSync.class);

    private static volatile ManagedReportGenerateSync INSTANCE;

    private final ManagedReportRepository reportRepository;

    public ManagedReportGenerateSync(ManagedReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    public static void onSuccess(Integer reportId, String filePath) {
        ManagedReportGenerateSync sync = INSTANCE;
        if (sync == null || reportId == null) {
            return;
        }
        try {
            sync.doSuccess(reportId.longValue(), filePath);
        } catch (Exception e) {
            logger.error("managed report sync success failed, reportId=" + reportId, e);
        }
    }

    public static void onFailure(Integer reportId, String message) {
        ManagedReportGenerateSync sync = INSTANCE;
        if (sync == null || reportId == null) {
            return;
        }
        try {
            sync.doFailure(reportId.longValue(), message);
        } catch (Exception e) {
            logger.error("managed report sync failure failed, reportId=" + reportId, e);
        }
    }

    private void doSuccess(Long id, String filePath) {
        Optional<ManagedReport> opt = reportRepository.findByIdAndDeleted(id, 0);
        if (!opt.isPresent()) {
            return;
        }
        ManagedReport report = opt.get();
        if (!Boolean.TRUE.equals(isManagedGenerate(report))) {
            // 仅当处于生成中时回写，避免误伤非本通道任务；外部 createReport 同 id 极少
            if (report.getGenerateStatus() == null
                    || report.getGenerateStatus() != ManagedReport.STATUS_GENERATING) {
                return;
            }
        }
        String oldPath = report.getFilePath();
        report.setGenerateStatus(ManagedReport.STATUS_SUCCESS);
        report.setFilePath(filePath);
        report.setFailMessage(null);
        reportRepository.save(report);
        if (StringUtils.isNotBlank(oldPath) && filePath != null && !oldPath.equals(filePath)) {
            deleteQuietly(oldPath);
        }
    }

    private void doFailure(Long id, String message) {
        Optional<ManagedReport> opt = reportRepository.findByIdAndDeleted(id, 0);
        if (!opt.isPresent()) {
            return;
        }
        ManagedReport report = opt.get();
        if (report.getGenerateStatus() == null
                || report.getGenerateStatus() != ManagedReport.STATUS_GENERATING) {
            return;
        }
        report.setGenerateStatus(ManagedReport.STATUS_FAILED);
        report.setFailMessage(truncate(message, 512));
        reportRepository.save(report);
    }

    private static Boolean isManagedGenerate(ManagedReport report) {
        return report.getGenerateStatus() != null
                && report.getGenerateStatus() == ManagedReport.STATUS_GENERATING;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }

    private static void deleteQuietly(String path) {
        try {
            File f = new File(path);
            if (f.isFile() && !f.delete()) {
                logger.warn("failed to delete old report file: " + path);
            }
        } catch (Exception e) {
            logger.warn("delete old report file error: " + path, e);
        }
    }
}
