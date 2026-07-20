package com.myreport.framework.redis;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis 连接配置，基于 Jedis 连接池。
 * <p>
 * 优先读取环境变量（与 application.yml / .env 一致）：
 * REDIS_HOST、REDIS_PORT、REDIS_PASSWORD、REDIS_DB
 */
public class RedisConfig {

    private static final Logger logger = Logger.getLogger(RedisConfig.class);

    private static volatile RedisConfig instance;

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private int timeout = 2000;
    private int maxTotal = 50;
    private int maxIdle = 20;
    private int minIdle = 5;
    private long maxWaitMillis = 3000L;

    private JedisPool jedisPool;

    private RedisConfig() {
        loadFromEnv();
        initPool();
    }

    public static RedisConfig getInstance() {
        if (instance == null) {
            synchronized (RedisConfig.class) {
                if (instance == null) {
                    instance = new RedisConfig();
                }
            }
        }
        return instance;
    }

    private void loadFromEnv() {
        host = firstNonBlank(System.getenv("REDIS_HOST"), host);
        port = parseInt(System.getenv("REDIS_PORT"), port);
        password = firstNonBlank(System.getenv("REDIS_PASSWORD"), password);
        database = parseInt(System.getenv("REDIS_DB"), database);
    }

    private static String firstNonBlank(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
    }

    private static int parseInt(String value, int defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void initPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWaitMillis(maxWaitMillis);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);

        if (StringUtils.isNotBlank(password)) {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, null, database);
        }
        logger.info("Redis pool initialized, host=" + host + ", port=" + port + ", database=" + database);
    }

    public Jedis getJedis() {
        return jedisPool.getResource();
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public int getDatabase() {
        return database;
    }

    public int getTimeout() {
        return timeout;
    }
}
