package com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.model.AzureOpenaiChatModel;
import com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.model.AzureOpenaiEmbedding;
import com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.model.AzureOpenaiImageModel;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class AzureModelProvider  extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
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
        modelInfos.add(new ModelInfo("Azure OpenAI","", ModelTypeEnum.LLM.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4","", ModelTypeEnum.LLM.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o","", ModelTypeEnum.LLM.name(),AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","", ModelTypeEnum.LLM.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-3-large","", ModelTypeEnum.EMBEDDING.name(),AzureOpenaiEmbedding.class));
        modelInfos.add(new ModelInfo("text-embedding-3-small","", ModelTypeEnum.EMBEDDING.name(),AzureOpenaiEmbedding.class));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","", ModelTypeEnum.EMBEDDING.name(),AzureOpenaiEmbedding.class));
        modelInfos.add(new ModelInfo("gpt-4o","",ModelTypeEnum.IMAGE.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","",ModelTypeEnum.IMAGE.name(), AzureOpenaiChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("dall-e-3","",ModelTypeEnum.TTI.name(), AzureOpenaiImageModel.class));
        //modelInfos.add(new ModelInfo("tts","",ModelTypeEnum.TTS.name(), new CosyVoiceTTS()));
        //modelInfos.add(new ModelInfo("whisper","", ModelTypeEnum.STT.name(), new ParaFormerSTT()));
        return modelInfos;
    }


}
