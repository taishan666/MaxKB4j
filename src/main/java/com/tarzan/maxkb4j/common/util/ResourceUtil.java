package com.tarzan.maxkb4j.common.util;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domain.dto.MaxKb4J;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    /**
     * 从 Resource 解析 MaxKb4J 对象
     */
    public static MaxKb4J parseMk(@NonNull Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return parseMk(is);
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取资源: " + resource, e);
        }
    }

    /**
     * 从 InputStream 解析 MaxKb4J 对象（自动关闭流）
     * 注意：此方法会关闭传入的 inputStream
     */
    public static MaxKb4J parseMk(@NonNull InputStream inputStream) {
        String text;
        // 显式使用 UTF-8 编码，避免平台依赖
        text = IoUtil.readToString(inputStream, StandardCharsets.UTF_8);
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("输入内容为空");
        }
        try {
            return JSONObject.parseObject(text, MaxKb4J.class);
        } catch (JSONException e) {
            throw new IllegalArgumentException("JSON 格式无效: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("解析 JSON 时发生未知错误", e);
        }
    }

    /**
     * 从 InputStream 中提取应用描述信息
     */
    public static ApplicationEntity parseApp(@NonNull Resource resource) {
        MaxKb4J mk = parseMk(resource); // 注意：此调用会关闭 inputStream
        if (mk == null || mk.getApplication() == null) {
            return null;
        }
        return mk.getApplication();
    }

    /**
     * 从 InputStream 中提取应用描述信息
     */
    public static String parseMkDesc(@NonNull Resource resource) {
        MaxKb4J mk = parseMk(resource); // 注意：此调用会关闭 inputStream
        if (mk == null || mk.getApplication() == null) {
            return "";
        }
        return mk.getApplication().getDesc() != null ? mk.getApplication().getDesc() : "";
    }
}