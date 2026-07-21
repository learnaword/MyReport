package com.myreport.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.config.SimpleReportProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 简化报告 HTTP 拉数（SSRF 防护 + 超时/体积限制）。
 */
@Component
public class SimpleReportHttpFetcher {

    private static final Logger logger = Logger.getLogger(SimpleReportHttpFetcher.class);

    private final SimpleReportProperties properties;

    public SimpleReportHttpFetcher(SimpleReportProperties properties) {
        this.properties = properties;
    }

    public JSONObject fetch(String method, String urlStr, String queryJson, String bodyJson) {
        String fullUrl = buildUrl(urlStr, queryJson);
        validateUrl(fullUrl);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            String m = StringUtils.isBlank(method) ? "GET" : method.trim().toUpperCase();
            conn.setRequestMethod(m);
            conn.setConnectTimeout(properties.getConnectTimeoutMs());
            conn.setReadTimeout(properties.getReadTimeoutMs());
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept", "application/json");

            if ("POST".equals(m) && StringUtils.isNotBlank(bodyJson)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] bytes = bodyJson.getBytes(StandardCharsets.UTF_8);
                conn.getOutputStream().write(bytes);
            }

            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = readLimited(in, properties.getMaxResponseBytes());
            if (code < 200 || code >= 300) {
                throw new IllegalArgumentException("HTTP " + code + (StringUtils.isBlank(body) ? "" : (": " + truncate(body, 120))));
            }
            if (StringUtils.isBlank(body)) {
                throw new IllegalArgumentException("空响应");
            }
            Object parsed = JSON.parse(body);
            if (!(parsed instanceof JSONObject)) {
                throw new IllegalArgumentException("响应须为 JSON 对象");
            }
            return (JSONObject) parsed;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("simple-report fetch failed: " + fullUrl, e);
            throw new IllegalArgumentException("请求失败：" + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String buildUrl(String urlStr, String queryJson) {
        if (StringUtils.isBlank(urlStr)) {
            throw new IllegalArgumentException("url 不能为空");
        }
        String base = urlStr.trim();
        if (StringUtils.isBlank(queryJson)) {
            return base;
        }
        JSONObject q;
        try {
            Object o = JSON.parse(queryJson);
            if (!(o instanceof JSONObject)) {
                throw new IllegalArgumentException("queryJson 须为对象");
            }
            q = (JSONObject) o;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("queryJson 不是合法 JSON");
        }
        StringBuilder sb = new StringBuilder(base);
        boolean first = !base.contains("?");
        for (String key : q.keySet()) {
            Object val = q.get(key);
            if (val == null) {
                continue;
            }
            sb.append(first ? "?" : "&");
            first = false;
            sb.append(encode(key)).append("=").append(encode(String.valueOf(val)));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public void validateUrl(String urlStr) {
        URI uri;
        try {
            uri = new URI(urlStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("URL 不合法");
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("仅允许 http/https");
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("URL 缺少 host");
        }
        String hostLower = host.toLowerCase();
        String path = uri.getPath() == null ? "" : uri.getPath();
        // 本服务联调假数据：始终允许本机访问 /demo/stat/*
        boolean demoLocal = isLoopbackHost(hostLower) && path.startsWith("/demo/stat");
        if (!demoLocal && !properties.isAllowLoopback()) {
            if (isLoopbackHost(hostLower) || "metadata.google.internal".equals(hostLower)) {
                throw new IllegalArgumentException("禁止访问本机或元数据地址");
            }
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                for (InetAddress addr : addrs) {
                    if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                            || addr.isSiteLocalAddress() || isUniqueLocal(addr)) {
                        throw new IllegalArgumentException("禁止访问内网地址");
                    }
                    byte[] b = addr.getAddress();
                    if (b.length == 4 && (b[0] & 0xFF) == 169 && (b[1] & 0xFF) == 254) {
                        throw new IllegalArgumentException("禁止访问链路本地地址");
                    }
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("无法解析 host：" + host);
            }
        } else if ("metadata.google.internal".equals(hostLower)) {
            throw new IllegalArgumentException("禁止访问元数据地址");
        }

        // demo 本机路径跳过 host 后缀白名单
        if (demoLocal) {
            return;
        }

        List<String> suffixes = properties.getAllowedHostSuffixes();
        if (suffixes != null && !suffixes.isEmpty()) {
            boolean ok = false;
            for (String suffix : suffixes) {
                if (StringUtils.isBlank(suffix)) {
                    continue;
                }
                String s = suffix.trim().toLowerCase();
                if (s.startsWith(".")) {
                    if (hostLower.endsWith(s) || hostLower.equals(s.substring(1))) {
                        ok = true;
                        break;
                    }
                } else if (hostLower.equals(s) || hostLower.endsWith("." + s)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new IllegalArgumentException("URL host 不在白名单内");
            }
        }
    }

    private static boolean isLoopbackHost(String hostLower) {
        return "localhost".equals(hostLower)
                || "127.0.0.1".equals(hostLower)
                || "0.0.0.0".equals(hostLower)
                || "::1".equals(hostLower);
    }

    private static boolean isUniqueLocal(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b.length != 16) {
            return false;
        }
        return (b[0] & 0xFE) == 0xFC;
    }

    private static String readLimited(InputStream in, int max) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > max) {
                throw new IllegalArgumentException("响应超过大小限制");
            }
            bos.write(buf, 0, n);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }
}
