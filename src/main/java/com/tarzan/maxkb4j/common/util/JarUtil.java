package com.tarzan.maxkb4j.common.util;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class JarUtil {

    public static String getParentDirName(Resource resource) throws IOException {
        URL url = resource.getURL();
        // 处理 JAR 内部路径：如 file:/app.jar!/templates/tool/mytool/file.tool
        if ("jar".equals(url.getProtocol())) {
            // 格式：jar:file:/path/to/app.jar!/templates/tool/xxx/file.tool
            String jarPath = url.getPath();
            // 截取 ! 之后的部分
            int idx = jarPath.indexOf("!/");
            if (idx != -1) {
                String innerPath = jarPath.substring(idx + 2); // 去掉 "!/"
                // 按 "/" 分割，取倒数第二段（即父目录）
                String[] segments = innerPath.split("/");
                if (segments.length >= 2) {
                    return segments[segments.length - 2];
                }
            }
            throw new IllegalStateException("无法解析 JAR 内资源路径: " + url);
        } else {
            // 文件系统路径
            return new File(resource.getURI()).getParentFile().getName();
        }
    }

    public static String getParentDirPath(Resource resource) throws IOException {
        URL url = resource.getURL();
        // 处理 JAR 内部路径：如 jar:file:/app.jar!/templates/tool/mytool/file.tool
        if ("jar".equals(url.getProtocol())) {
            String jarPath = url.getPath(); // 格式：/path/to/app.jar!/templates/tool/mytool/file.tool
            int idx = jarPath.indexOf("!/");
            if (idx != -1) {
                String innerPath = jarPath.substring(idx + 2); // 去掉 "!/"，得到 templates/tool/mytool/file.tool
                // 获取父路径：去掉最后一段（文件名）
                int lastSlash = innerPath.lastIndexOf('/');
                if (lastSlash > 0) {
                    return innerPath.substring(0, lastSlash); // 返回如 "templates/tool/mytool"
                } else if (lastSlash == 0) {
                    return ""; // 根目录下，如 "/file.txt" -> 父路径为 ""
                } else {
                    // 没有 '/'，说明路径不合法或只有文件名
                    throw new IllegalStateException("无法解析 JAR 内资源路径（无目录结构）: " + url);
                }
            }
            throw new IllegalStateException("无法解析 JAR 内资源路径: " + url);
        } else {
            // 文件系统路径
            File parentFile = new File(resource.getURI()).getParentFile();
            if (parentFile == null) {
                throw new IllegalStateException("资源无父目录: " + url);
            }
            return parentFile.getAbsolutePath(); // 或者用 getCanonicalPath() 如果需要标准化路径
        }
    }
}
