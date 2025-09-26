package com.tarzan.maxkb4j.module.model.provider.impl.wenxinmodelprovider;


import com.tarzan.maxkb4j.module.model.provider.*;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_wenxin_provider")
public class WenXinModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.WenXin.getProvider());
        info.setName(ModelProviderEnum.WenXin.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/wenxin_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("ERNIE-Bot-4","","LLM",null));
        modelInfos.add(new ModelInfo("ERNIE-Bot","","LLM",null));
        modelInfos.add(new ModelInfo("ERNIE-Bot-turbo","","LLM",null));
        modelInfos.add(new ModelInfo("Embedding-V1","","EMBEDDING",null));
        return modelInfos;
    }
    
}
