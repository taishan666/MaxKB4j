package com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider.model.DeepSeekChatModel;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_deepseek_provider")
public class DeepSeekModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.DeepSeek.getProvider());
        info.setName(ModelProviderEnum.DeepSeek.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/deepseek_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("deepseek-chat","", ModelType.LLM.name(), DeepSeekChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("deepseek-reasoner","", ModelType.LLM.name(), DeepSeekChatModel.class,new LlmModelParams()));
        return modelInfos;
    }


}
