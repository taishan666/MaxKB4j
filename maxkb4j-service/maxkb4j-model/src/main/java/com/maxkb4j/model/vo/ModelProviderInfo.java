package com.maxkb4j.model.vo;

import com.maxkb4j.common.util.IoUtil;
import lombok.Data;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ModelProviderInfo {

    private static final String MODEL_ICONS_PATH = "model-icons/";

    private static final Map<String, String> SVG_CACHE = new ConcurrentHashMap<>();

    private String provider;
    private String name;
    private String icon;

    public ModelProviderInfo(String provider, String name, String icon) {
        this.provider = provider;
        this.name = name;
        this.icon = loadSvgIcon(icon);
    }

    /**
     * 懒加载 SVG 图标：首次使用时按需读取并缓存，替代原先在静态块中遍历 {@code ModelProvider.values()} 预加载的方式。
     */
    private static String loadSvgIcon(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return SVG_CACHE.computeIfAbsent(name, ModelProviderInfo::readSvgIcon);
    }

    private static String readSvgIcon(String name) {
        ClassLoader classLoader = ModelProviderInfo.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(MODEL_ICONS_PATH + name);
        return inputStream != null ? IoUtil.readToString(inputStream) : null;
    }

}