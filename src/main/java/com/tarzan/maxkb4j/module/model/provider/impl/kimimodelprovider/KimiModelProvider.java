package com.tarzan.maxkb4j.module.model.provider.impl.kimimodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.ModelInfoManage;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.kimimodelprovider.model.KimiChatModel;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_kimi_provider")
public class KimiModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Kimi.getProvider());
        info.setName(ModelProviderEnum.Kimi.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/kimi_icon.svg");
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
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("moonshot-v1-8k","","LLM",KimiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("moonshot-v1-32k","","LLM",KimiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("moonshot-v1-128k","","LLM",KimiChatModel.class,new LlmModelParams()));
        return modelInfos;
    }

}
