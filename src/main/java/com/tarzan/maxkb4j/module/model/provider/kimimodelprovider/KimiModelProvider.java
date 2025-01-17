package com.tarzan.maxkb4j.module.model.provider.kimimodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_kimi_provider")
public class KimiModelProvider implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_kimi_provider");
        info.setName("Kimi");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/kimi_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("moonshot-v1-8k","","LLM"));
        modelInfos.add(new ModelInfo("moonshot-v1-32k","","LLM"));
        return modelInfos;
    }
}
