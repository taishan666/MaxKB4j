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

@Component("model_xf_provider")
public class XfModelProvider  extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.XunFei.getProvider());
        info.setName(ModelProviderEnum.XunFei.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/xf_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("glm-4","", ModelType.LLM));
        modelInfos.add(new ModelInfo("glm-4v","",ModelType.LLM));
        modelInfos.add(new ModelInfo("glm-3-turbo","",ModelType.LLM));
        modelInfos.add(new ModelInfo("glm-4v-plus","",ModelType.IMAGE_UNDERSTANDING));
        modelInfos.add(new ModelInfo("glm-4v-flash","",ModelType.IMAGE_UNDERSTANDING));
        modelInfos.add(new ModelInfo("cogview-3","",ModelType.TTI));
        modelInfos.add(new ModelInfo("cogview-3-plus","",ModelType.TTI));
        return modelInfos;
    }


}
