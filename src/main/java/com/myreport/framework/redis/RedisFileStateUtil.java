package com.myreport.framework.redis;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * 报告文件生成进度状态，存于 Redis。
 * <p>
 * nState: 1-处理中, 2-成功, 4-失败
 */
public final class RedisFileStateUtil {

    private static final Logger logger = Logger.getLogger(RedisFileStateUtil.class);

    /** Redis key 过期时间（秒），默认 7 天 */
    private static final int DEFAULT_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    private RedisFileStateUtil() {
    }

    /**
     * 初始化文件进度。
     *
     * @param key        Redis key
     * @param filePath   物理路径或待生成文件路径
     * @param createTime 创建时间
     * @param progress   当前进度
     * @param total      总进度（如指标数）
     * @param message    附加信息
     * @param state      状态：1 处理中
     */
    public static void fileProgress(String key, String filePath, String createTime,
                                    int progress, int total, String message, int state) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        JSONObject json = new JSONObject();
        json.put("strPhysicsUrl", StringUtils.defaultString(filePath));
        json.put("strCreateTime", StringUtils.defaultString(createTime));
        json.put("strConsumeTime", "");
        json.put("nProgress", progress);
        json.put("nTotal", total);
        json.put("strMessage", StringUtils.defaultString(message));
        json.put("strProcessInfo", "");
        json.put("nState", state);
        save(key, json);
    }

    /**
     * 更新文件进度。
     *
     * @param key          Redis key
     * @param createTime   创建时间（空则不覆盖）
     * @param consumeTime  耗时（空则不覆盖）
     * @param progressDelta 进度增量，&gt;0 时累加到 nProgress
     * @param physicsUrl   物理地址/OSS 地址（空则不覆盖）
     * @param state        状态
     */
    public static void fileUpdateProgress(String key, String createTime, String consumeTime,
                                          int progressDelta, String physicsUrl, int state) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        JSONObject json = loadOrCreate(key);
        if (StringUtils.isNotBlank(createTime)) {
            json.put("strCreateTime", createTime);
        }
        if (StringUtils.isNotBlank(consumeTime)) {
            json.put("strConsumeTime", consumeTime);
        }
        if (progressDelta > 0) {
            int current = json.containsKey("nProgress") ? json.getIntValue("nProgress") : 0;
            json.put("nProgress", current + progressDelta);
        }
        if (StringUtils.isNotBlank(physicsUrl)) {
            json.put("strPhysicsUrl", physicsUrl);
        }
        json.put("nState", state);
        save(key, json);
    }

    /**
     * 追加过程信息并更新状态（异常收集等场景）。
     *
     * @param key         Redis key
     * @param processInfo 过程/异常信息
     * @param state       状态，通常为 1
     */
    public static void fileUpdateProgress(String key, String processInfo, int state) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        JSONObject json = loadOrCreate(key);
        if (StringUtils.isNotBlank(processInfo)) {
            String oldInfo = json.getString("strProcessInfo");
            if (StringUtils.isBlank(oldInfo)) {
                json.put("strProcessInfo", processInfo);
            } else {
                json.put("strProcessInfo", oldInfo + "\n" + processInfo);
            }
            json.put("strMessage", processInfo);
        }
        json.put("nState", state);
        save(key, json);
    }

    public static JSONObject getFileState(String key) {
        if (StringUtils.isBlank(key) || !RedisTemplate.exists(key)) {
            return null;
        }
        String jsonStr = RedisTemplate.getStringValue(key);
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONObject.parseObject(jsonStr);
        } catch (Exception e) {
            logger.error("Parse file state failed, key=" + key, e);
            return null;
        }
    }

    private static JSONObject loadOrCreate(String key) {
        JSONObject json = getFileState(key);
        if (json != null) {
            return json;
        }
        JSONObject created = new JSONObject();
        created.put("strPhysicsUrl", "");
        created.put("strCreateTime", "");
        created.put("strConsumeTime", "");
        created.put("nProgress", 0);
        created.put("nTotal", 0);
        created.put("strMessage", "");
        created.put("strProcessInfo", "");
        created.put("nState", 1);
        return created;
    }

    private static void save(String key, JSONObject json) {
        try {
            RedisTemplate.setStringValue(key, json.toJSONString(), DEFAULT_EXPIRE_SECONDS);
        } catch (Exception e) {
            logger.error("Save file state failed, key=" + key, e);
        }
    }
}
