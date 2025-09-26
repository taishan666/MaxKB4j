package com.tarzan.maxkb4j.module.model.provider.impl.geminimodelprovider;

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

@Component("model_gemini_provider")
public class GeminiModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.Gemini.getProvider());
        info.setName(ModelProviderEnum.Gemini.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/gemini_icon.svg");
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
        modelInfos.add(new ModelInfo("gemini-1.0-pro","","LLM",null));
        modelInfos.add(new ModelInfo("gemini-1.0-pro-visio","","LLM",null));
        modelInfos.add(new ModelInfo("models/embedding-001","","EMBEDDING",null));
        modelInfos.add(new ModelInfo("gemini-1.5-flash","","STT",null));
        modelInfos.add(new ModelInfo("gemini-1.5-flash","","IMAGE",null));
        return modelInfos;
    }
    
}
