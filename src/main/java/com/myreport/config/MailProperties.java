package com.myreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件发送配置（默认按 QQ 邮箱 SMTP）。
 */
@ConfigurationProperties(prefix = "myreport.mail")
public class MailProperties {

    /** 总开关；false 或未配账号时不发信 */
    private boolean enabled = false;

    private String host = "smtp.qq.com";

    private int port = 465;

    /** 发件人 QQ 邮箱，如 xxx@qq.com */
    private String username = "";

    /** QQ 邮箱「授权码」（非登录密码） */
    private String password = "";

    /** 发件人显示名 */
    private String fromName = "MyReport";

    private boolean smtpAuth = true;

    private boolean sslEnable = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public boolean isSslEnable() {
        return sslEnable;
    }

    public void setSslEnable(boolean sslEnable) {
        this.sslEnable = sslEnable;
    }

    public boolean isReady() {
        return enabled
                && username != null && username.trim().length() > 0
                && password != null && password.trim().length() > 0
                && host != null && host.trim().length() > 0;
    }
}
