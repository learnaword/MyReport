package com.myreport.config;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 模版封面/底图上传目录的静态访问映射。
 */
@Configuration
public class ReportTemplateUploadConfig implements WebMvcConfigurer {

    public static final String URL_PREFIX = "/files/report-template/";

    @Value("${myreport.template-upload-dir:}")
    private String templateUploadDir;

    public File getUploadRoot() {
        if (StringUtils.isNotBlank(templateUploadDir)) {
            return new File(templateUploadDir.trim());
        }
        return new File(System.getProperty("user.home"), "myreport/uploads");
    }

    public File getTemplateImageRoot() {
        return new File(getUploadRoot(), "report-template");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File root = getTemplateImageRoot();
        if (!root.exists()) {
            root.mkdirs();
        }
        String location = root.toURI().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(URL_PREFIX + "**")
                .addResourceLocations(location);
    }
}
