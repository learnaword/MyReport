package com.myreport.service;

import com.myreport.config.MailProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化报告邮件通知（QQ SMTP）。
 */
@Service
public class SimpleReportMailService {

    private static final Logger logger = Logger.getLogger(SimpleReportMailService.class);

    private final MailProperties mailProperties;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public SimpleReportMailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    /**
     * @return null 表示跳过或成功；非空为失败原因摘要
     */
    public String sendReportAttachment(String toEmails, String reportName, String filePath) {
        if (!mailProperties.isReady() || mailSender == null) {
            logger.info("mail not ready, skip send");
            return "邮件未启用或未配置 SMTP（跳过发送）";
        }
        List<String> tos = parseEmails(toEmails);
        if (tos.isEmpty()) {
            return null;
        }
        File file = new File(filePath);
        if (!file.isFile()) {
            return "附件文件不存在";
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String from = mailProperties.getUsername().trim();
            String fromName = StringUtils.isBlank(mailProperties.getFromName())
                    ? "MyReport"
                    : mailProperties.getFromName().trim();
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(tos.toArray(new String[0]));
            String title = StringUtils.isBlank(reportName) ? "简化报告" : reportName.trim();
            helper.setSubject("【MyReport】" + title + " 已生成");
            helper.setText("您好，\n\n简化报告「" + title + "」已生成，请查收附件。\n\n— MyReport", false);
            helper.addAttachment(file.getName(), new FileSystemResource(file));
            mailSender.send(message);
            logger.info("simple-report mail sent to " + tos);
            return null;
        } catch (Exception e) {
            logger.error("simple-report mail send failed", e);
            return "发信失败：" + e.getMessage();
        }
    }

    static List<String> parseEmails(String raw) {
        List<String> list = new ArrayList<String>();
        if (StringUtils.isBlank(raw)) {
            return list;
        }
        String[] parts = raw.split("[,;；，\\s]+");
        for (String p : parts) {
            if (StringUtils.isBlank(p)) {
                continue;
            }
            String e = p.trim();
            if (e.contains("@") && e.length() <= 128) {
                list.add(e);
            }
        }
        return list;
    }
}
