package com.myreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 简化报告配置。
 */
@ConfigurationProperties(prefix = "myreport.simple-report")
public class SimpleReportProperties {

    /** 交付目录白名单根（空则使用 files 下 report/simple-delivery/） */
    private String deliveryRoot = "";

    /** 连接超时 ms */
    private int connectTimeoutMs = 10000;

    /** 读取超时 ms */
    private int readTimeoutMs = 30000;

    /** 响应体上限字节 */
    private int maxResponseBytes = 2 * 1024 * 1024;

    /** 是否允许拉本机/内网（本地联调默认 true；生产请改 false） */
    private boolean allowLoopback = true;

    /** 允许的 host 后缀（空则仅拦内网/回环，不额外限后缀） */
    private List<String> allowedHostSuffixes = new ArrayList<String>();

    public String getDeliveryRoot() {
        return deliveryRoot;
    }

    public void setDeliveryRoot(String deliveryRoot) {
        this.deliveryRoot = deliveryRoot;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(int maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    public boolean isAllowLoopback() {
        return allowLoopback;
    }

    public void setAllowLoopback(boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    public List<String> getAllowedHostSuffixes() {
        return allowedHostSuffixes;
    }

    public void setAllowedHostSuffixes(List<String> allowedHostSuffixes) {
        this.allowedHostSuffixes = allowedHostSuffixes;
    }
}
