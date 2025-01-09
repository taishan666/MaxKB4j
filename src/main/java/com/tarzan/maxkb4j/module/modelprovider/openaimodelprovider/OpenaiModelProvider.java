package com.tarzan.maxkb4j.module.modelprovider.openaimodelprovider;

import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_openai_provider")
public class OpenaiModelProvider implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_openai_provider");
        info.setName("OpenAI");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/openai_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gpt-3.5-turbo","","LLM"));
        modelInfos.add(new ModelInfo("gpt-4","","LLM"));
        modelInfos.add(new ModelInfo("gpt-4o","","LLM"));
        modelInfos.add(new ModelInfo("gpt-4o-mini","","LLM"));
        modelInfos.add(new ModelInfo("gpt-4-turbo","","LLM"));
        modelInfos.add(new ModelInfo("gpt-4-turbo-preview","","LLM"));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","","EMBEDDING"));
        modelInfos.add(new ModelInfo("whisper-1","","STT"));
        modelInfos.add(new ModelInfo("tts-1","","TTS"));
        modelInfos.add(new ModelInfo("gpt-4o","","IMAGE"));
        modelInfos.add(new ModelInfo("qwen-vl-max","","IMAGE"));
        modelInfos.add(new ModelInfo("dall-e-2","","TTI"));
        return modelInfos;
    }
}
