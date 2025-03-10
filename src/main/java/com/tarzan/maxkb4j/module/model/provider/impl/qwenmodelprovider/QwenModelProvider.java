package com.tarzan.maxkb4j.module.model.provider.impl.qwenmodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelInfoManage;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.*;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class QwenModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.QWen.getProvider());
        info.setName(ModelProviderEnum.QWen.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/qwen_icon.svg");
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
        modelInfos.add(new ModelInfo("qwen-turbo","", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-plus","", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-max","", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("text-embedding-v2","", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("text-embedding-v3","", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","", ModelTypeEnum.STT.name(),new BaiLianSpeechToText()));
        modelInfos.add(new ModelInfo("cosyvoice-v1","",ModelTypeEnum.TTS.name(),new BaiLianTextToSpeech()));
        modelInfos.add(new ModelInfo("qwen-vl-plus","",ModelTypeEnum.IMAGE.name(),new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-vl-max","",ModelTypeEnum.IMAGE.name(),new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("wanx2.1-t2i-turbo","",ModelTypeEnum.TTI.name(),new QWenImageModel()));
        modelInfos.add(new ModelInfo("gte-rerank","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }

}
