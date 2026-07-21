package com.myreport.util;

/**
 * 项目常量。
 */
public final class Constant {

    private Constant() {
    }

    /**
     * Redis Key 模板，配合 String.format 使用。
     */
    public static final class RedisKey {

        private RedisKey() {
        }

        /** 报告下载/生成进度，参数：reportId */
        public static final String REPORT_LIST = "report:list:%d";

        /** 报告生成锁，参数：reportId */
        public static final String REPORT_LOCK = "report:lock:%d";

        /** 报告生成执行中数量，参数：serverId */
        public static final String REPORT_CREATE_EXECUTOR_COUNT = "report:create:executor:count:%d";

        /** Excel 导入进度，参数：taskId */
        public static final String IMPORT_PROGRESS = "import:progress:%s";
    }
}
