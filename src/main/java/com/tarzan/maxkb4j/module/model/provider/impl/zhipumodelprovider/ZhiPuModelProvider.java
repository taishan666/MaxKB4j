package com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
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

@Component("model_zhipu_provider")
public class ZhiPuModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.ZhiPu.getProvider());
        info.setName(ModelProviderEnum.ZhiPu.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/zhipuai_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("glm-4","", ModelTypeEnum.LLM.name(),new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("glm-4v","", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("glm-3-turbo","", ModelTypeEnum.LLM.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("text-embedding-v3","", ModelTypeEnum.EMBEDDING.name(),new BaiLianEmbedding()));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","", ModelTypeEnum.STT.name(), new ParaFormerSTT()));
        modelInfos.add(new ModelInfo("cosyvoice-v1","",ModelTypeEnum.TTS.name(), new CosyVoiceTTS()));
        modelInfos.add(new ModelInfo("glm-4v-plus","",ModelTypeEnum.IMAGE.name(),new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("glm-4v","",ModelTypeEnum.IMAGE.name(), new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("glm-4v-flash","",ModelTypeEnum.IMAGE.name(),new BaiLianChatModel()));
        modelInfos.add(new ModelInfo("cogview-3","",ModelTypeEnum.TTI.name(),new QWenImageModel()));
        modelInfos.add(new ModelInfo("cogview-3-plus","",ModelTypeEnum.TTI.name(),new QWenImageModel()));
        modelInfos.add(new ModelInfo("cogview-3-flash","",ModelTypeEnum.TTI.name(),new QWenImageModel()));
        modelInfos.add(new ModelInfo("gte-rerank","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }

}
