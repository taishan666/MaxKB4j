package com.tarzan.maxkb4j.module.modelprovider.xfmodelprovider;

import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_xf_provider")
public class XfModelProvider  implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_xf_provider");
        info.setName("讯飞星火");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/xf_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("glm-4","","LLM"));
        modelInfos.add(new ModelInfo("glm-4v","","LLM"));
        modelInfos.add(new ModelInfo("glm-3-turbo","","LLM"));
        modelInfos.add(new ModelInfo("glm-4v-plus","","IMAGE"));
        modelInfos.add(new ModelInfo("glm-4v-flash","","IMAGE"));
        modelInfos.add(new ModelInfo("cogview-3","","TTI"));
        modelInfos.add(new ModelInfo("cogview-3-plus","","TTI"));
        return modelInfos;
    }
}
