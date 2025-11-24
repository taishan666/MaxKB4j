package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_tencent_provider")
public class TencentModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Tencent.getProvider());
        info.setName(ModelProviderEnum.Tencent.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/tencent_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("hunyuan-pro","", ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-standard","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-lite","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-role","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-functioncall","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-code","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-embedding","",ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("hunyuan-vision","",ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("hunyuan-dit","", ModelType.TTI));
        return modelInfos;
    }

}
