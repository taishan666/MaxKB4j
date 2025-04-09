package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ImageModelParams;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.*;
import com.tarzan.maxkb4j.util.IoUtil;

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
        modelInfos.add(new ModelInfo("qwen-turbo","大语言模型", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-plus","大语言模型", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-max","大语言模型", ModelTypeEnum.LLM.name(),new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("text-embedding-v2","文本向量模型", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("text-embedding-v3","文本向量模型", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","语音识别模型", ModelTypeEnum.STT.name(), new BaiLianSpeechToText()));
        modelInfos.add(new ModelInfo("cosyvoice-v1","语言生成模型",ModelTypeEnum.TTS.name(),new BaiLianTextToSpeech()));
        modelInfos.add(new ModelInfo("qwen-vl-plus","AI视觉模型",ModelTypeEnum.IMAGE.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-vl-max","AI视觉模型",ModelTypeEnum.IMAGE.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("wanx2.1-t2i-turbo","文生图模型",ModelTypeEnum.TTI.name(),new QWenImageModel(),new ImageModelParams()));
        modelInfos.add(new ModelInfo("wanx2.1-imageedit","图生图模型",ModelTypeEnum.TTI.name(),new QWenImageModel(),new ImageModelParams()));
        modelInfos.add(new ModelInfo("gte-rerank","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }


}
