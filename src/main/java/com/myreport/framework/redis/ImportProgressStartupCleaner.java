package com.myreport.framework.redis;

import org.apache.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 服务启动时清理 Excel 导入进度，避免重启后前端仍恢复已中断任务的进度条。
 */
@Component
public class ImportProgressStartupCleaner implements ApplicationRunner {

    private static final Logger logger = Logger.getLogger(ImportProgressStartupCleaner.class);

    @Override
    public void run(ApplicationArguments args) {
        try {
            int n = ImportProgressUtil.clearAll();
            if (n > 0) {
                logger.info("cleared " + n + " import progress key(s) on startup");
            }
        } catch (Exception e) {
            logger.warn("clear import progress on startup failed", e);
        }
    }
}
