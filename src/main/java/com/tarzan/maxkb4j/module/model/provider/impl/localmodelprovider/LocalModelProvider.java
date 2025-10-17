package com.tarzan.maxkb4j.module.model.provider.impl.localmodelprovider;

import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.provider.BaseModelCredential;
import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.localmodelprovider.model.LocalEmbedding;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class LocalModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Local.getProvider());
        info.setName(ModelProviderEnum.Local.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/local_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("all-minilm-l6-v2","文本向量模型", ModelType.EMBEDDING.name(), LocalEmbedding.class));
        return modelInfos;
    }

    @Override
    public BaseModelCredential getModelCredential() {
        return new BaseModelCredential(false,false);
    }


}
