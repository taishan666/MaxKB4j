package com.tarzan.maxkb4j.module.model.provider.wenxinmodelprovider;


import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_wenxin_provider")
public class WenXinModelProvider implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_wenxin_provider");
        info.setName("千帆大模型");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/wenxin_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("ERNIE-Bot-4","","LLM"));
        modelInfos.add(new ModelInfo("ERNIE-Bot","","LLM"));
        modelInfos.add(new ModelInfo("ERNIE-Bot-turbo","","LLM"));
        modelInfos.add(new ModelInfo("Embedding-V1","","EMBEDDING"));
        return modelInfos;
    }
}
