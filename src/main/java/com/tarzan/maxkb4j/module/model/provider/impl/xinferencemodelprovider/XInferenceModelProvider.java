package com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider;

import com.tarzan.maxkb4j.module.model.provider.BaseModelCredential;
import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model.XinferenceChat;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class XInferenceModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
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
        modelInfos.add(new ModelInfo("qwen:7b","", ModelTypeEnum.LLM.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("llama3:8b","", ModelTypeEnum.LLM.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("deepseek-r1:8b","", ModelTypeEnum.LLM.name(),XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("bge-base-zh","", ModelTypeEnum.EMBEDDING.name(),XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("llava:7b","",ModelTypeEnum.IMAGE.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("llava:13b","",ModelTypeEnum.IMAGE.name(), XinferenceChat.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("sdxl-turbo","",ModelTypeEnum.TTI.name(), XinferenceChat.class,new LlmModelParams()));
       // modelInfos.add(new ModelInfo("linux6200/bge-reranker-v2-m3","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }

    @Override
    public BaseModelCredential getModelCredential() {
        return new BaseModelCredential(true,true);
    }

}
