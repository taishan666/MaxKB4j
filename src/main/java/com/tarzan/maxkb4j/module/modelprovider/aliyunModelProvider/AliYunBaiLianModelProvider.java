package com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider;

import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("aliyun_bai_lian_model_provider")
public class AliYunBaiLianModelProvider implements IModelProvider {

    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("aliyun_bai_lian_model_provider");
        info.setName("阿里云百炼");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/aliyun_bai_lian_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("qwen-turbo","","LLM"));
        modelInfos.add(new ModelInfo("qwen-plus","","LLM"));
        modelInfos.add(new ModelInfo("qwen-max","","LLM"));
        modelInfos.add(new ModelInfo("text-embedding-v2","","EMBEDDING"));
        modelInfos.add(new ModelInfo("text-embedding-v3","","EMBEDDING"));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","","STT"));
        modelInfos.add(new ModelInfo("cosyvoice-v1","","TTS"));
        modelInfos.add(new ModelInfo("qwen-vl-plus","","IMAGE"));
        modelInfos.add(new ModelInfo("qwen-vl-max","","IMAGE"));
        modelInfos.add(new ModelInfo("wanx-v1","","TTI"));
        modelInfos.add(new ModelInfo("gte-rerank","","RERANKER"));
        return modelInfos;
    }
}
