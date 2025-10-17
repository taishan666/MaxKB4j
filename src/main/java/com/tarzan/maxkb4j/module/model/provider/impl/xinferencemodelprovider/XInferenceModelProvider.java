package com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider;

import com.tarzan.maxkb4j.module.model.provider.BaseModelCredential;
import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model.XinferenceChat;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class XInferenceModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.XInference.getProvider());
        info.setName(ModelProviderEnum.XInference.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/xinference_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("qwen:7b","", ModelType.LLM.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("llama3:8b","", ModelType.LLM.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("deepseek-r1:8b","", ModelType.LLM.name(),XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("bge-base-zh","", ModelType.EMBEDDING.name(),XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("llava:7b","", ModelType.IMAGE.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("llava:13b","", ModelType.IMAGE.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("sdxl-turbo","", ModelType.TTI.name(), XinferenceChat.class,new LlmModelParams()));
       // modelInfos.add(new ModelInfo("linux6200/bge-reranker-v2-m3","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }

    @Override
    public BaseModelCredential getModelCredential() {
        return new BaseModelCredential(true,true);
    }

}
