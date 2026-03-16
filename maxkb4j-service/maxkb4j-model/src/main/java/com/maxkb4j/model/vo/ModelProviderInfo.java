package com.maxkb4j.model.vo;

import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.model.enums.ModelProvider;
import lombok.Data;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ModelProviderInfo {

    private static final String MODEL_ICONS_PATH = "model-icons/";

    private static final Map<String, String> SVG_CACHE = new ConcurrentHashMap<>();

    static {
        // 启动时预加载所有 SVG 图标
        for (ModelProvider provider : ModelProvider.values()) {
            loadSvgIcon(provider.getIcon());
        }
    }

    private String provider;
    private String name;
    private String icon;

    public ModelProviderInfo(String provider, String name, String icon) {
        this.provider = provider;
        this.name = name;
        this.icon = SVG_CACHE.get(icon);
    }

    private static void loadSvgIcon(String name) {
        ClassLoader classLoader = ModelProviderInfo.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(MODEL_ICONS_PATH + name);
        if (inputStream != null) {
            SVG_CACHE.put(name, IoUtil.readToString(inputStream));
        }
    }

}
