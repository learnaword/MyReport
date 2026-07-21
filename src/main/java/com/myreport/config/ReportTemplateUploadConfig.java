package com.myreport.config;

import java.io.File;

import javax.annotation.PostConstruct;

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

    private static volatile ReportTemplateUploadConfig INSTANCE;

    @Value("${myreport.template-upload-dir:}")
    private String templateUploadDir;

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    public File getUploadRoot() {
        if (StringUtils.isNotBlank(templateUploadDir)) {
            return new File(templateUploadDir.trim());
        }
        return new File(System.getProperty("user.home"), "myreport/uploads");
    }

    public File getTemplateImageRoot() {
        return new File(getUploadRoot(), "report-template");
    }

    /**
     * 将模版图 URL / 相对路径解析为本地文件；非本系统托管路径返回 null。
     * 支持：
     * <ul>
     *   <li>{@code /files/report-template/1/cover.png}</li>
     *   <li>{@code http://host:port/files/report-template/1/cover.png}</li>
     * </ul>
     */
    public static File resolveManagedImageFile(String imagePath) {
        if (StringUtils.isBlank(imagePath)) {
            return null;
        }
        String path = extractManagedRelativePath(imagePath.trim());
        if (path == null) {
            return null;
        }
        ReportTemplateUploadConfig cfg = INSTANCE;
        File root = cfg != null
                ? cfg.getTemplateImageRoot()
                : new File(System.getProperty("user.home"), "myreport/uploads/report-template");
        File file = new File(root, path);
        try {
            String canonical = file.getCanonicalPath();
            String rootCanon = root.getCanonicalPath();
            if (!canonical.equals(rootCanon) && !canonical.startsWith(rootCanon + File.separator)) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    private static String extractManagedRelativePath(String imagePath) {
        String pathPart = imagePath;
        if (pathPart.startsWith("http://") || pathPart.startsWith("https://")) {
            int schemeSep = pathPart.indexOf("://");
            if (schemeSep < 0) {
                return null;
            }
            int pathStart = pathPart.indexOf('/', schemeSep + 3);
            if (pathStart < 0) {
                return null;
            }
            pathPart = pathPart.substring(pathStart);
            int q = pathPart.indexOf('?');
            if (q >= 0) {
                pathPart = pathPart.substring(0, q);
            }
        }
        if (!pathPart.startsWith(URL_PREFIX)) {
            return null;
        }
        String relative = pathPart.substring(URL_PREFIX.length());
        if (relative.isEmpty() || relative.contains("..")
                || relative.startsWith("/") || relative.startsWith("\\")) {
            return null;
        }
        return relative;
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
