package com.tarzan.maxkb4j.module.model.provider.impl.xfmodelprovider;

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

@Component("model_xf_provider")
public class XfModelProvider  extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.XunFei.getProvider());
        info.setName(ModelProviderEnum.XunFei.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/xf_icon.svg");
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
        modelInfos.add(new ModelInfo("glm-4","","LLM",null));
        modelInfos.add(new ModelInfo("glm-4v","","LLM",null));
        modelInfos.add(new ModelInfo("glm-3-turbo","","LLM",null));
        modelInfos.add(new ModelInfo("glm-4v-plus","","IMAGE",null));
        modelInfos.add(new ModelInfo("glm-4v-flash","","IMAGE",null));
        modelInfos.add(new ModelInfo("cogview-3","","TTI",null));
        modelInfos.add(new ModelInfo("cogview-3-plus","","TTI",null));
        return modelInfos;
    }

}
