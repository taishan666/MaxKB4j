package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelInfoManage;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.credential.BaiLianLLMModelCredential;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.BaiLianChatModel;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.BaiLianEmbedding;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.BaiLianTextToSpeech;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model.QWenTextToImageModel;
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
        InputStream inputStream = classLoader.getResourceAsStream("icon/aliyun_bai_lian_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public ModelInfoManage getModelInfoManage() {
        return new ModelInfoManage(getModelList());
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("qwen-turbo","", ModelTypeEnum.LLM.name(),new BaiLianLLMModelCredential(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-plus","", ModelTypeEnum.LLM.name(),new BaiLianLLMModelCredential(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("qwen-max","", ModelTypeEnum.LLM.name(),new BaiLianLLMModelCredential(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("text-embedding-v2","", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("text-embedding-v3","", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","", ModelTypeEnum.STT.name(),new BaiLianLLMModelCredential(), new BaiLianTextToSpeech()));
        modelInfos.add(new ModelInfo("cosyvoice-v1","",ModelTypeEnum.TTS.name(),new BaiLianLLMModelCredential(), new BaiLianTextToSpeech()));
        modelInfos.add(new ModelInfo("qwen-vl-plus","",ModelTypeEnum.IMAGE.name(),null));
        modelInfos.add(new ModelInfo("qwen-vl-max","",ModelTypeEnum.IMAGE.name(),null));
        modelInfos.add(new ModelInfo("wanx2.1-t2i-turbo","",ModelTypeEnum.TTI.name(),new BaiLianLLMModelCredential(),new QWenTextToImageModel()));
        modelInfos.add(new ModelInfo("gte-rerank","",ModelTypeEnum.RERANKER.name(),null));
        return modelInfos;
    }


}
