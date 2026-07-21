package com.myreport.framework.redis;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Excel 导入任务进度（Redis）。
 * <p>
 * nState: 1-处理中, 2-成功, 4-失败
 */
public final class ImportProgressUtil {

    private static final Logger logger = Logger.getLogger(ImportProgressUtil.class);

    public static final int STATE_RUNNING = 1;
    public static final int STATE_SUCCESS = 2;
    public static final int STATE_FAILED = 4;

    private static final int EXPIRE_SECONDS = 24 * 60 * 60;

    private ImportProgressUtil() {
    }

    public static void start(String taskId, String type, String message) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        JSONObject json = new JSONObject();
        json.put("taskId", taskId);
        json.put("type", StringUtils.defaultString(type));
        json.put("nState", STATE_RUNNING);
        json.put("nProgress", 0);
        json.put("nTotal", 100);
        json.put("strMessage", StringUtils.defaultString(message, "任务已提交"));
        save(taskId, json);
    }

    public static void update(String taskId, int progress, int total, String message) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        JSONObject json = loadOrCreate(taskId);
        json.put("nState", STATE_RUNNING);
        json.put("nProgress", Math.max(0, progress));
        json.put("nTotal", total > 0 ? total : 100);
        if (message != null) {
            json.put("strMessage", message);
        }
        save(taskId, json);
    }

    public static void success(String taskId, String message, Map<String, Object> result) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        JSONObject json = loadOrCreate(taskId);
        json.put("nState", STATE_SUCCESS);
        json.put("nProgress", 100);
        json.put("nTotal", 100);
        json.put("strMessage", StringUtils.defaultString(message, "导入成功"));
        if (result != null) {
            json.put("result", result);
        }
        save(taskId, json);
    }

    public static void fail(String taskId, String message) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        JSONObject json = loadOrCreate(taskId);
        json.put("nState", STATE_FAILED);
        json.put("strMessage", StringUtils.defaultString(message, "导入失败"));
        save(taskId, json);
    }

    public static JSONObject get(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        String key = keyOf(taskId);
        if (!RedisTemplate.exists(key)) {
            return null;
        }
        String raw = RedisTemplate.getStringValue(key);
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return JSONObject.parseObject(raw);
        } catch (Exception e) {
            logger.error("Parse import progress failed, taskId=" + taskId, e);
            return null;
        }
    }

    /**
     * 清理全部导入进度（服务重启时调用，避免前端恢复已中断任务）。
     */
    public static int clearAll() {
        String pattern = String.format(com.myreport.util.Constant.RedisKey.IMPORT_PROGRESS, "*");
        java.util.Set<String> keys = RedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (String key : keys) {
            RedisTemplate.delete(key);
            n++;
        }
        return n;
    }

    private static JSONObject loadOrCreate(String taskId) {
        JSONObject existing = get(taskId);
        if (existing != null) {
            return existing;
        }
        JSONObject json = new JSONObject();
        json.put("taskId", taskId);
        json.put("type", "");
        json.put("nState", STATE_RUNNING);
        json.put("nProgress", 0);
        json.put("nTotal", 100);
        json.put("strMessage", "");
        return json;
    }

    private static void save(String taskId, JSONObject json) {
        try {
            RedisTemplate.setStringValue(keyOf(taskId), json.toJSONString(), EXPIRE_SECONDS);
        } catch (Exception e) {
            logger.error("Save import progress failed, taskId=" + taskId, e);
        }
    }

    private static String keyOf(String taskId) {
        return String.format(com.myreport.util.Constant.RedisKey.IMPORT_PROGRESS, taskId);
    }
}
