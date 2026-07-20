package com.myreport.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件路径与基础文件操作工具。
 */
public class FileUtil {

    public static final FileConfig fileConfig = new FileConfig();

    /**
     * 将文件列表打包为 zip。
     *
     * @param filePathList    待打包文件绝对路径列表
     * @param fullZipFilePath zip 输出绝对路径
     */
    public static void packfileToZip(List<?> filePathList, String fullZipFilePath) throws IOException {
        if (filePathList == null || filePathList.isEmpty()) {
            return;
        }
        File zipFile = new File(fullZipFilePath);
        File parent = zipFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            byte[] buffer = new byte[8192];
            for (Object item : filePathList) {
                if (item == null) {
                    continue;
                }
                File file = new File(String.valueOf(item));
                if (!file.exists() || !file.isFile()) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(file.getName()));
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 删除文件列表中的文件（忽略不存在的路径）。
     */
    public static void deleteFileList(List<?> filePathList) {
        if (filePathList == null || filePathList.isEmpty()) {
            return;
        }
        for (Object item : filePathList) {
            if (item == null) {
                continue;
            }
            File file = new File(String.valueOf(item));
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        }
    }

    /**
     * 文件服务 / 物理路径配置。
     */
    public static class FileConfig {
        /** 本地文件物理根路径 */
        private String prefixFilePhysicalPath;
        /** 文件服务访问前缀路径 */
        private String fileSvrPrePath;

        public FileConfig() {
            String defaultPath = System.getProperty("myreport.file.path",
                    System.getProperty("user.dir") + File.separator + "files" + File.separator);
            if (!defaultPath.endsWith("/") && !defaultPath.endsWith("\\")) {
                defaultPath = defaultPath + File.separator;
            }
            this.prefixFilePhysicalPath = defaultPath;
            this.fileSvrPrePath = defaultPath;
        }

        public String getPrefixFilePhysicalPath() {
            return prefixFilePhysicalPath;
        }

        public void setPrefixFilePhysicalPath(String prefixFilePhysicalPath) {
            this.prefixFilePhysicalPath = prefixFilePhysicalPath;
        }

        public String getFileSvrPrePath() {
            return fileSvrPrePath;
        }

        public void setFileSvrPrePath(String fileSvrPrePath) {
            this.fileSvrPrePath = fileSvrPrePath;
        }
    }
}
