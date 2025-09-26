package com.tarzan.maxkb4j.module.model.provider.impl.localmodelprovider;

import com.tarzan.maxkb4j.module.model.provider.*;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class LocalModelProvider extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.Local.getProvider());
        info.setName(ModelProviderEnum.Local.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/local_icon.svg");
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
        return List.of();
    }


}
