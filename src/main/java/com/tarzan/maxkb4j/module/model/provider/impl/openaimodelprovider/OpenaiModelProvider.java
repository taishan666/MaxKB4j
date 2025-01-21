package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider;

import com.tarzan.maxkb4j.module.model.provider.*;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
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
    public ModelInfoManage getModelInfoManage() {
        return null;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gpt-3.5-turbo","","LLM",null));
        modelInfos.add(new ModelInfo("gpt-4","","LLM",null));
        modelInfos.add(new ModelInfo("gpt-4o","","LLM",null));
        modelInfos.add(new ModelInfo("gpt-4o-mini","","LLM",null));
        modelInfos.add(new ModelInfo("gpt-4-turbo","","LLM",null));
        modelInfos.add(new ModelInfo("gpt-4-turbo-preview","","LLM",null));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","","EMBEDDING",null));
        modelInfos.add(new ModelInfo("whisper-1","","STT",null));
        modelInfos.add(new ModelInfo("tts-1","","TTS",null));
        modelInfos.add(new ModelInfo("gpt-4o","","IMAGE",null));
        modelInfos.add(new ModelInfo("qwen-vl-max","","IMAGE",null));
        modelInfos.add(new ModelInfo("dall-e-2","","TTI",null));
        return modelInfos;
    }

}
