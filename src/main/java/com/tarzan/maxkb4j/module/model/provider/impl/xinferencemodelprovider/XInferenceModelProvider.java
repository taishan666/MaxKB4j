package com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelInfoManage;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model.XinferenceChat;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model.XinferenceEmbedding;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model.XinferenceImage;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class XInferenceModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.XInference.getProvider());
        info.setName(ModelProviderEnum.XInference.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/xinference_icon.svg");
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
        modelInfos.add(new ModelInfo("qwen:7b","", ModelTypeEnum.LLM.name(), new XinferenceChat()));
        modelInfos.add(new ModelInfo("llama3:8b","", ModelTypeEnum.LLM.name(), new XinferenceChat()));
        modelInfos.add(new ModelInfo("deepseek-r1:8b","", ModelTypeEnum.LLM.name(),new XinferenceChat()));
        modelInfos.add(new ModelInfo("bge-base-zh","", ModelTypeEnum.EMBEDDING.name(),new XinferenceEmbedding()));
        modelInfos.add(new ModelInfo("llava:7b","",ModelTypeEnum.IMAGE.name(), new XinferenceChat()));
        modelInfos.add(new ModelInfo("llava:13b","",ModelTypeEnum.IMAGE.name(), new XinferenceChat()));
        modelInfos.add(new ModelInfo("sdxl-turbo","",ModelTypeEnum.TTI.name(), new XinferenceImage()));
       // modelInfos.add(new ModelInfo("linux6200/bge-reranker-v2-m3","",ModelTypeEnum.RERANKER.name(),new BaiLianReranker()));
        return modelInfos;
    }

}
