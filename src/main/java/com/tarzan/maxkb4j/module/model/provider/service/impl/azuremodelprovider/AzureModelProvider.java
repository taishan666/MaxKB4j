package com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider;

import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.dto.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.model.AzureOpenaiChatModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.model.AzureOpenaiEmbedding;
import com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.model.AzureOpenaiImageModel;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class AzureModelProvider  extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Azure.getProvider());
        info.setName(ModelProviderEnum.Azure.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/azure_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("Azure OpenAI","", ModelType.LLM.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4","", ModelType.LLM.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o","", ModelType.LLM.name(),AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","", ModelType.LLM.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-3-large","", ModelType.EMBEDDING.name(),AzureOpenaiEmbedding.class));
        modelInfos.add(new ModelInfo("text-embedding-3-small","", ModelType.EMBEDDING.name(),AzureOpenaiEmbedding.class));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","", ModelType.EMBEDDING.name(),AzureOpenaiEmbedding.class));
        modelInfos.add(new ModelInfo("gpt-4o","", ModelType.IMAGE.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","", ModelType.IMAGE.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("dall-e-3","", ModelType.TTI.name(), AzureOpenaiImageModel.class));
        //modelInfos.add(new ModelInfo("tts","",ModelTypeEnum.TTS.name(), new CosyVoiceTTS()));
        //modelInfos.add(new ModelInfo("whisper","", ModelTypeEnum.STT.name(), new ParaFormerSTT()));
        return modelInfos;
    }


}
