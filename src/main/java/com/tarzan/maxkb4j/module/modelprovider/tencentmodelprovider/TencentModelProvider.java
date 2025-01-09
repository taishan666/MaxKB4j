package com.tarzan.maxkb4j.module.modelprovider.tencentmodelprovider;

import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_tencent_provider")
public class TencentModelProvider implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_tencent_provider");
        info.setName("腾讯混元");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/tencent_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("hunyuan-pro","","LLM"));
        modelInfos.add(new ModelInfo("hunyuan-standard","","LLM"));
        modelInfos.add(new ModelInfo("hunyuan-lite","","LLM"));
        modelInfos.add(new ModelInfo("hunyuan-role","","LLM"));
        modelInfos.add(new ModelInfo("hunyuan-functioncall","","LLM"));
        modelInfos.add(new ModelInfo("hunyuan-code","","LLM"));
        modelInfos.add(new ModelInfo("hunyuan-embedding","","EMBEDDING"));
        modelInfos.add(new ModelInfo("hunyuan-vision","","IMAGE"));
        modelInfos.add(new ModelInfo("hunyuan-dit","","TTI"));
        return modelInfos;
    }
}
