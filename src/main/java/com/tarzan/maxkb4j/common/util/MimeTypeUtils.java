package com.tarzan.maxkb4j.common.util;

import java.util.HashMap;
import java.util.Map;

public class MimeTypeUtils {

    private static final Map<String, String> mimeTypeMap = new HashMap<>();

    static {
        mimeTypeMap.put("jpg", "image/jpeg");
        mimeTypeMap.put("jpeg", "image/jpeg");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("gif", "image/gif");
        // 可以根据需要添加更多的文件类型和对应的MIME类型
    }

    /**
     * 获取给定扩展名的MIME类型，如果未找到，则返回默认值"image/jpeg"
     *
     * @param extension 文件扩展名
     * @return MIME类型
     */
    public static String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "image/jpeg";
        }
        return mimeTypeMap.getOrDefault(extension.toLowerCase(), "image/jpeg");
    }
}