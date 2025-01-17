package com.tarzan.maxkb4j.module.model.provider.geminimodelprovider;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_gemini_provider")
public class GeminiModelProvider implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_gemini_provider");
        info.setName("Gemini");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/gemini_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gemini-1.0-pro","","LLM"));
        modelInfos.add(new ModelInfo("gemini-1.0-pro-visio","","LLM"));
        modelInfos.add(new ModelInfo("models/embedding-001","","EMBEDDING"));
        modelInfos.add(new ModelInfo("gemini-1.5-flash","","STT"));
        modelInfos.add(new ModelInfo("gemini-1.5-flash","","IMAGE"));
        return modelInfos;
    }
}
