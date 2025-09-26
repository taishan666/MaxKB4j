package com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.model.ZhiPuChatModel;
import com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.model.ZhiPuEmbedding;
import com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.model.ZhiPuImageModel;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
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
        modelInfos.add(new ModelInfo("glm-4","", ModelTypeEnum.LLM.name(),ZhiPuChatModel.class, new LlmModelParams()));
        modelInfos.add(new ModelInfo("glm-4v","", ModelTypeEnum.LLM.name(), ZhiPuChatModel.class, new LlmModelParams()));
        modelInfos.add(new ModelInfo("glm-3-turbo","", ModelTypeEnum.LLM.name(), ZhiPuChatModel.class, new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-v3","", ModelTypeEnum.EMBEDDING.name(), ZhiPuEmbedding.class));
        modelInfos.add(new ModelInfo("glm-4v-plus","",ModelTypeEnum.IMAGE.name(), ZhiPuImageModel.class));
        modelInfos.add(new ModelInfo("glm-4v","",ModelTypeEnum.IMAGE.name(), ZhiPuChatModel.class, new LlmModelParams()));
        modelInfos.add(new ModelInfo("glm-4v-flash","",ModelTypeEnum.IMAGE.name(),ZhiPuChatModel.class, new LlmModelParams()));
        modelInfos.add(new ModelInfo("cogview-3","",ModelTypeEnum.TTI.name(),ZhiPuImageModel.class));
        modelInfos.add(new ModelInfo("cogview-3-plus","",ModelTypeEnum.TTI.name(),ZhiPuImageModel.class));
        modelInfos.add(new ModelInfo("cogview-3-flash","",ModelTypeEnum.TTI.name(),ZhiPuImageModel.class));
        return modelInfos;
    }

}
