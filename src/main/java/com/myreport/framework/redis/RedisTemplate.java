package com.myreport.framework.redis;

import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

/**
 * Redis 操作封装，提供项目中常用的静态方法。
 */
public final class RedisTemplate {

    private static final Logger logger = Logger.getLogger(RedisTemplate.class);

    private RedisTemplate() {
    }

    public static boolean exists(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            return Boolean.TRUE.equals(jedis.exists(key));
        } catch (Exception e) {
            logger.error("Redis exists failed, key=" + key, e);
            return false;
        } finally {
            closeQuietly(jedis);
        }
    }

    public static String getStringValue(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Redis get failed, key=" + key, e);
            return null;
        } finally {
            closeQuietly(jedis);
        }
    }

    public static void setStringValue(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            jedis.set(key, value);
        } catch (Exception e) {
            logger.error("Redis set failed, key=" + key, e);
        } finally {
            closeQuietly(jedis);
        }
    }

    public static void setStringValue(String key, String value, int seconds) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            jedis.setex(key, seconds, value);
        } catch (Exception e) {
            logger.error("Redis setex failed, key=" + key, e);
        } finally {
            closeQuietly(jedis);
        }
    }

    public static Long delete(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            return jedis.del(key);
        } catch (Exception e) {
            logger.error("Redis delete failed, key=" + key, e);
            return 0L;
        } finally {
            closeQuietly(jedis);
        }
    }

    public static Long incr(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            return jedis.incr(key);
        } catch (Exception e) {
            logger.error("Redis incr failed, key=" + key, e);
            return null;
        } finally {
            closeQuietly(jedis);
        }
    }

    public static Long decr(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisConfig.getInstance().getJedis();
            return jedis.decr(key);
        } catch (Exception e) {
            logger.error("Redis decr failed, key=" + key, e);
            return null;
        } finally {
            closeQuietly(jedis);
        }
    }

    private static void closeQuietly(Jedis jedis) {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (Exception e) {
                logger.warn("Failed to close jedis", e);
            }
        }
    }
}
