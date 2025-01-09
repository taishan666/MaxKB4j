package com.tarzan.maxkb4j.module.modelprovider.deepseekmodelprovider;

import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_deepseek_provider")
public class DeepSeekModelProvider implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_deepseek_provider");
        info.setName("DeepSeek");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/deepseek_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("deepseek-chat","","LLM"));
        modelInfos.add(new ModelInfo("deepseek-coder","","LLM"));
        return modelInfos;
    }
}
