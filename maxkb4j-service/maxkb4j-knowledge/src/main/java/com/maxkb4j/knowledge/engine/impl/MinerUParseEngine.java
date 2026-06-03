package com.maxkb4j.knowledge.engine.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.maxkb4j.knowledge.engine.DocumentParseEngine;
import com.maxkb4j.knowledge.engine.props.MinerUProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MinerU 文档解析引擎（自托管模式）
 * <p>
 * 调用自建 MinerU 服务（/file_parse，同步返回）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinerUParseEngine implements DocumentParseEngine {

    public static final String NAME = "mineru";

    private static final String SELF_HOSTED_PARSE_PATH = "/file_parse";

    private final MinerUProperties properties;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean support(String fileName) {
        if (!properties.isEnabled() || fileName == null) {
            return false;
        }
        if (StrUtil.isBlank(properties.getApiUrl())) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return properties.getSupportedExtensions().stream().anyMatch(lower::endsWith);
    }

    @Override
    public String extractText(String fileName, InputStream inputStream) {
        byte[] bytes = IoUtil.readBytes(inputStream);
        return extractTextSelfHosted(fileName, bytes);
    }

    private String extractTextSelfHosted(String fileName, byte[] bytes) {
        if (StrUtil.isBlank(properties.getApiUrl())) {
            throw new IllegalStateException("MinerU api-url is not configured");
        }
        String url = buildUrl(properties.getApiUrl(), SELF_HOSTED_PARSE_PATH);

        Map<String, Object> form = new HashMap<>();
        form.put("file", new ByteArrayInputStream(bytes));
        form.put("language", properties.getLanguage());
        form.put("enable_ocr", String.valueOf(properties.isEnableOcr()));
        form.put("enable_table", String.valueOf(properties.isEnableTable()));
        form.put("enable_formula", String.valueOf(properties.isEnableFormula()));
        form.put("max_convert_pages", String.valueOf(properties.getMaxConvertPages()));

        HttpRequest request = HttpUtil.createPost(url)
                .timeout(properties.getTimeout())
                .form(form);
        if (StrUtil.isNotBlank(properties.getToken())) {
            request.header("Authorization", "Bearer " + properties.getToken());
        }
        request.header("X-Filename", fileName);

        try (HttpResponse response = request.execute()) {
            if (!response.isOk()) {
                throw new RuntimeException("MinerU 解析失败, status=" + response.getStatus()
                        + ", body=" + response.body());
            }
            String body = new String(response.bodyBytes(), StandardCharsets.UTF_8);
            return parseMarkdown(body);
        } catch (Exception e) {
            log.error("调用 MinerU 解析文件 {} 失败", fileName, e);
            throw new RuntimeException("调用 MinerU 解析失败: " + e.getMessage(), e);
        }
    }

    private String buildUrl(String base, String path) {
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    /**
     * 兼容 MinerU 多种返回格式：
     * 1) 纯字符串 markdown
     * 2) {"markdown": "..."} / {"md_content": "..."} / {"content": "..."}
     * 3) {"data": {"markdown": "..."}}
     * 4) {"results": [ { "md_content": "..." } ]}
     */
    private String parseMarkdown(String body) {
        if (StrUtil.isBlank(body)) {
            return "";
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }
        JSONObject json;
        try {
            json = JSONObject.parseObject(trimmed);
        } catch (Exception e) {
            return trimmed;
        }
        String md = extractMarkdownField(json);
        if (StrUtil.isNotBlank(md)) {
            return md;
        }
        JSONObject data = json.getJSONObject("data");
        if (data != null) {
            md = extractMarkdownField(data);
            if (StrUtil.isNotBlank(md)) {
                return md;
            }
        }
        JSONArray results = json.getJSONArray("results");
        if (results == null && data != null) {
            results = data.getJSONArray("results");
        }
        if (results != null && !results.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                String text = extractMarkdownField(item);
                if (StrUtil.isNotBlank(text)) {
                    if (!sb.isEmpty()) sb.append("\n\n");
                    sb.append(text);
                }
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }
        return trimmed;
    }

    private String extractMarkdownField(JSONObject obj) {
        for (String key : new String[]{"markdown", "md_content", "content", "text"}) {
            String val = obj.getString(key);
            if (StrUtil.isNotBlank(val)) {
                return val;
            }
        }
        return null;
    }
}
