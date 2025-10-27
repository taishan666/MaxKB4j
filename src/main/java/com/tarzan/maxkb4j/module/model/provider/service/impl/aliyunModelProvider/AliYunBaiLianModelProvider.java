package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider;

import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.dto.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.model.*;
import com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.params.*;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.WanxModelName;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AliYunBaiLianModelProvider extends IModelProvider {

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.AliYunBaiLian.getProvider());
        info.setName(ModelProviderEnum.AliYunBaiLian.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/qwen_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }



    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_TURBO,"大语言模型", ModelType.LLM.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_PLUS,"大语言模型", ModelType.LLM.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_MAX,"大语言模型", ModelType.LLM.name(),BaiLianChatModel.class,new LlmModelParams()));
       // modelInfos.add(new ModelInfo(QwenModelName.TEXT_EMBEDDING_V2,"文本向量模型", ModelType.EMBEDDING.name(),BaiLianEmbedding.class));
        modelInfos.add(new ModelInfo(QwenModelName.TEXT_EMBEDDING_V3,"文本向量模型", ModelType.EMBEDDING.name(),BaiLianEmbedding.class));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","语音识别模型", ModelType.STT.name(), ParaFormerRealtimeSTT.class));
        modelInfos.add(new ModelInfo("paraformer-v2","语音识别模型", ModelType.STT.name(), ParaFormerSTT.class));
        modelInfos.add(new ModelInfo("gummy-realtime-v1","语音识别模型", ModelType.STT.name(), GummySTT.class,new GummySTTParams()));
        modelInfos.add(new ModelInfo("sensevoice-v1","语音识别模型", ModelType.STT.name(), SenseVoiceSTT.class));
        modelInfos.add(new ModelInfo("cosyvoice-v1","语音生成模型", ModelType.TTS.name(),CosyVoiceTTS.class,new CosyVoiceV1TTSParams()));
        modelInfos.add(new ModelInfo("cosyvoice-v2","语音生成模型", ModelType.TTS.name(),CosyVoiceTTS.class,new CosyVoiceV2TTSParams()));
        modelInfos.add(new ModelInfo("sambert-v1","语音生成模型", ModelType.TTS.name(),SamBertTTS.class,new SamBertTTSParams()));
        modelInfos.add(new ModelInfo("qwen-tts","语音生成模型", ModelType.TTS.name(),QWenTTS.class,new QWenTTSParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_VL_PLUS,"AI视觉模型", ModelType.IMAGE.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_VL_MAX,"AI视觉模型", ModelType.IMAGE.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo(WanxModelName.WANX2_1_T2I_TURBO,"文生图模型", ModelType.TTI.name(),QWenImageModel.class,new WanXImageModelParams()));
        modelInfos.add(new ModelInfo(WanxModelName.WANX2_1_T2I_PLUS,"文生图模型", ModelType.TTI.name(),QWenImageModel.class,new WanXImageModelParams()));
        modelInfos.add(new ModelInfo("wanx2.1-imageedit","图生图模型", ModelType.TTI.name(),QWenImageModel.class,new WanXImageEditModelParams()));
        modelInfos.add(new ModelInfo("gte-rerank","", ModelType.RERANKER.name(),BaiLianReranker.class));
        return modelInfos;
    }


}
