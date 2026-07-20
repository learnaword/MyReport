package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONObject;
import com.myreport.util.FileUtil;

public class CommonUtil {

    /**
     * 返回报告磁盘路径
     */
    public static String getBasePath(JSONObject overallSetting) {
        String strBasePathPre = overallSetting.getString("strBasePathPre");
        String basePath = FileUtil.fileConfig.getPrefixFilePhysicalPath() + strBasePathPre + "/";
        return basePath;
    }

    public static String getBasePath(String strBasePathPre) {
        String basePath = FileUtil.fileConfig.getPrefixFilePhysicalPath() + strBasePathPre + "/";
        return basePath;
    }

}
