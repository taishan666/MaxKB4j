package com.tarzan.maxkb4j.module.model.provider.vo;

import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import lombok.Data;

@Data
public class ModelProviderInfo {

    private String provider;
    private String name;
    private String icon;

    public ModelProviderInfo(ModelProviderEnum modelProvider) {
        this.provider = modelProvider.getProvider();
        this.name = modelProvider.getName();
    }

}
