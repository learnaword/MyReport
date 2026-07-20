package com.myreport.framework.redis;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Redis 连接配置，基于 Jedis 连接池。
 */
public class RedisConfig {

    private static final Logger logger = Logger.getLogger(RedisConfig.class);
    private static final String CONFIG_FILE = "redis.properties";

    private static volatile RedisConfig instance;

    private String host = "127.0.0.1";
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
        loadProperties();
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

    private void loadProperties() {
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE);
            if (in == null) {
                in = RedisConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            }
            if (in != null) {
                props.load(in);
                host = props.getProperty("redis.host", host);
                port = Integer.parseInt(props.getProperty("redis.port", String.valueOf(port)));
                password = props.getProperty("redis.password", password);
                database = Integer.parseInt(props.getProperty("redis.database", String.valueOf(database)));
                timeout = Integer.parseInt(props.getProperty("redis.timeout", String.valueOf(timeout)));
                maxTotal = Integer.parseInt(props.getProperty("redis.pool.maxTotal", String.valueOf(maxTotal)));
                maxIdle = Integer.parseInt(props.getProperty("redis.pool.maxIdle", String.valueOf(maxIdle)));
                minIdle = Integer.parseInt(props.getProperty("redis.pool.minIdle", String.valueOf(minIdle)));
                maxWaitMillis = Long.parseLong(props.getProperty("redis.pool.maxWaitMillis", String.valueOf(maxWaitMillis)));
            } else {
                logger.warn("redis.properties not found, using default Redis config");
            }
        } catch (Exception e) {
            logger.error("Failed to load redis.properties, using defaults", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
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
