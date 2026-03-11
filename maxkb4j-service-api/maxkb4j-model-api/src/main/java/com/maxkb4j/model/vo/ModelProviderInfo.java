package com.maxkb4j.model.vo;

import com.maxkb4j.common.util.IoUtil;
import lombok.Data;

import java.io.InputStream;

@Data
public class ModelProviderInfo {

    private static final String MODEL_ICONS_PATH = "model-icons/";

    private String provider;
    private String name;
    private String icon;

    public ModelProviderInfo(String provider, String name, String icon) {
        this.provider = provider;
        this.name = name;
        this.icon = getSvgIcon(icon);
    }

    /**
     * Gets SVG icon for the provider
     * @param name the icon name
     * @return the SVG icon as string
     */
    public String getSvgIcon(String name) {
        ClassLoader classLoader = ModelProviderInfo.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(MODEL_ICONS_PATH + name);
        if (inputStream == null) {
            return null;
        }
        return IoUtil.readToString(inputStream);
    }

}
