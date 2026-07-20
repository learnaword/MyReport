package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    /**
     * 获取当前时间
     */
    public static String getReportCreateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date());
    }

    /**
     * 获取报告消耗时间
     */
    public static String getConsumeTime(JSONObject overallSetting) {
        Long startTime = overallSetting.getLong("mergeFileToken");
        Long consumeMillis = System.currentTimeMillis() - startTime;
        long s = consumeMillis / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        String result = String.format("%02d:%02d:%02d", h, m, sec);
        return result;
    }
}
