package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.*;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.params.*;
import com.tarzan.maxkb4j.util.IoUtil;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.WanxModelName;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AliYunBaiLianModelProvider extends IModelProvider {

    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
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
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_TURBO,"大语言模型", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_PLUS,"大语言模型", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_MAX,"大语言模型", ModelTypeEnum.LLM.name(),new BaiLianChatModel()));
      //  modelInfos.add(new ModelInfo(QwenModelName.TEXT_EMBEDDING_V2,"文本向量模型", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo(QwenModelName.TEXT_EMBEDDING_V3,"文本向量模型", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","语音识别模型", ModelTypeEnum.STT.name(), new BaiLianSpeechToText()));
        modelInfos.add(new ModelInfo("paraformer-v2","语音识别模型", ModelTypeEnum.STT.name(), new BaiLianSpeechToText()));
        modelInfos.add(new ModelInfo("cosyvoice-v1","语音生成模型",ModelTypeEnum.TTS.name(),new CosyVoiceTTS(),new CosyVoiceTTSParams()));
        modelInfos.add(new ModelInfo("sambert-v1","语音生成模型",ModelTypeEnum.TTS.name(),new SamBertTTS(),new SamBertTTSParams()));
        modelInfos.add(new ModelInfo("qwen-tts","语音生成模型",ModelTypeEnum.TTS.name(),new QWenTTS(),new QWenTTSParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_VL_PLUS,"AI视觉模型",ModelTypeEnum.IMAGE.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_VL_MAX,"AI视觉模型",ModelTypeEnum.IMAGE.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo(WanxModelName.WANX2_1_T2I_TURBO,"文生图模型",ModelTypeEnum.TTI.name(),new QWenImageModel(),new WanXImageModelParams()));
        modelInfos.add(new ModelInfo(WanxModelName.WANX2_1_T2I_PLUS,"文生图模型",ModelTypeEnum.TTI.name(),new QWenImageModel(),new WanXImageModelParams()));
       // modelInfos.add(new ModelInfo("stable-diffusion-3.5-large-turbo","文生图模型",ModelTypeEnum.TTI.name(),new QWenImageModel(),new ImageModelParams()));
        modelInfos.add(new ModelInfo("wanx2.1-imageedit","图生图模型",ModelTypeEnum.TTI.name(),new QWenImageModel(),new WanXImageEditModelParams()));
        modelInfos.add(new ModelInfo("gte-rerank","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }


}
