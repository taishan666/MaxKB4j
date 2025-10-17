package com.tarzan.maxkb4j.module.model.provider.service.impl.qwenmodelprovider;

import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.provider.dto.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.model.*;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class QwenModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.QWen.getProvider());
        info.setName(ModelProviderEnum.QWen.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/qwen_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("qwen-turbo","", ModelType.LLM.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("qwen-plus","", ModelType.LLM.name(),  BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("qwen-max","", ModelType.LLM.name(),  BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-v2","", ModelType.EMBEDDING.name(), BaiLianEmbedding.class));
        modelInfos.add(new ModelInfo("text-embedding-v3","", ModelType.EMBEDDING.name(),BaiLianEmbedding.class));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","", ModelType.STT.name(),ParaFormerSTT.class));
        modelInfos.add(new ModelInfo("cosyvoice-v1","", ModelType.TTS.name(),CosyVoiceTTS.class));
        modelInfos.add(new ModelInfo("qwen-vl-plus","", ModelType.IMAGE.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("qwen-vl-max","", ModelType.IMAGE.name(), BaiLianChatModel.class,new LlmModelParams()));
        modelInfos.add(new ModelInfo("wanx2.1-t2i-turbo","", ModelType.TTI.name(),QWenImageModel.class));
        modelInfos.add(new ModelInfo("gte-rerank","", ModelType.RERANKER.name(),BaiLianReranker.class));
        return modelInfos;
    }

}
