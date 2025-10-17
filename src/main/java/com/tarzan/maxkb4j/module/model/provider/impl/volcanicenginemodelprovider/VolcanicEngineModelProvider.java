package com.tarzan.maxkb4j.module.model.provider.impl.volcanicenginemodelprovider;

import com.tarzan.maxkb4j.module.model.provider.*;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class VolcanicEngineModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.VolcanicEngine.getProvider());
        info.setName(ModelProviderEnum.VolcanicEngine.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/volcanic_engine_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public ModelInfoManage getModelInfoManage() {
        return null;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return List.of();
    }

}
