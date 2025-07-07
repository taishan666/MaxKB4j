package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model.OpenaiChatModel;
import com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model.OpenaiEmbedding;
import com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model.OpenaiImageModel;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_openai_provider")
public class OpenaiModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.Openai.getProvider());
        info.setName(ModelProviderEnum.Openai.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/openai_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gpt-3.5-turbo","","LLM",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("gpt-4","","LLM",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("gpt-4o","","LLM",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","","LLM",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("gpt-4-turbo","","LLM",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("gpt-4-turbo-preview","","LLM",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","","EMBEDDING",new OpenaiEmbedding()));
        modelInfos.add(new ModelInfo("whisper-1","","STT",null));
        modelInfos.add(new ModelInfo("tts-1","","TTS",null));
        modelInfos.add(new ModelInfo("gpt-4o","","IMAGE",new OpenaiChatModel()));
        modelInfos.add(new ModelInfo("dall-e-2","","TTI",new OpenaiImageModel()));
        return modelInfos;
    }

}
