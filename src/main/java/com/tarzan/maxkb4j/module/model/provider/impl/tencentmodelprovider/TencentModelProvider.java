package com.tarzan.maxkb4j.module.model.provider.impl.tencentmodelprovider;

import com.tarzan.maxkb4j.module.model.provider.*;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_tencent_provider")
public class TencentModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Tencent.getProvider());
        info.setName(ModelProviderEnum.Tencent.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/tencent_icon.svg");
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
        modelInfos.add(new ModelInfo("hunyuan-pro","","LLM",null));
        modelInfos.add(new ModelInfo("hunyuan-standard","","LLM",null));
        modelInfos.add(new ModelInfo("hunyuan-lite","","LLM",null));
        modelInfos.add(new ModelInfo("hunyuan-role","","LLM",null));
        modelInfos.add(new ModelInfo("hunyuan-functioncall","","LLM",null));
        modelInfos.add(new ModelInfo("hunyuan-code","","LLM",null));
        modelInfos.add(new ModelInfo("hunyuan-embedding","","EMBEDDING",null));
        modelInfos.add(new ModelInfo("hunyuan-vision","","IMAGE",null));
        modelInfos.add(new ModelInfo("hunyuan-dit","","TTI",null));
        return modelInfos;
    }
    
}
