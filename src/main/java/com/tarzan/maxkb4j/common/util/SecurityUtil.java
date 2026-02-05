package com.tarzan.maxkb4j.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

/**
 * 安全工具类
 * 提供各种安全相关的工具方法
 *
 * @author tarzan
 */
@Slf4j
@UtilityClass
public class SecurityUtil {

    /**
     * 规范化文件路径，防止路径穿越攻击
     *
     * @param path 输入的文件路径
     * @return 规范化的路径，如果路径无效则返回null
     */
    public static String normalizeFilePath(String path) {
        if (path == null) {
            return null;
        }
        try {
            // 使用Java的Path API进行路径标准化
            java.nio.file.Path normalizedPath = Paths.get(path).normalize();
            String normalizedString = normalizedPath.toString();
            // 检查是否包含相对路径符号，这可能是路径穿越的迹象
            if (normalizedString.contains("../") || normalizedString.contains("..\\") || normalizedString.startsWith("..")) {
                log.warn("检测到潜在的路径穿越攻击: {}", path);
                return null; // 拒绝可能的路径穿越尝试
            }

            return normalizedString;
        } catch (Exception e) {
            log.warn("路径标准化失败: {}", path, e);
            return null;
        }
    }

    /**
     * 检查文件名是否有效（不包含路径穿越字符）
     *
     * @param filename 输入的文件名
     * @return 如果文件名有效返回true，否则返回false
     */
    public static boolean validFileName(String filename) {
        if (filename == null) {
            return true;
        }

        // 检查是否包含路径穿越字符
        return filename.contains("../") || filename.contains("..\\") ||
                filename.startsWith("..") || filename.contains("..");
    }
}