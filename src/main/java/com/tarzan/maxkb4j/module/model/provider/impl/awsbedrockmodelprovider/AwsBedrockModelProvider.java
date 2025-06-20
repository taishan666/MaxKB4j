package com.tarzan.maxkb4j.module.model.provider.impl.awsbedrockmodelprovider;

import com.tarzan.maxkb4j.module.model.provider.*;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_aws_bedrock_provider")
public class AwsBedrockModelProvider  extends IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider(ModelProviderEnum.AwsBedrock.getProvider());
        info.setName(ModelProviderEnum.AwsBedrock.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/bedrock_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("anthropic.claude-v2:1","","LLM",null));
        modelInfos.add(new ModelInfo("anthropic.claude-v2","","LLM",null));
        modelInfos.add(new ModelInfo("anthropic.claude-3-haiku-20240307-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("anthropic.claude-3-sonnet-20240229-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("anthropic.claude-3-5-sonnet-20240620-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("anthropic.claude-instant-v1","","LLM",null));
        modelInfos.add(new ModelInfo("amazon.titan-text-premier-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("amazon.titan-text-lite-v1","","LLM",null));
        modelInfos.add(new ModelInfo("amazon.titan-text-express-v1","","LLM",null));
        modelInfos.add(new ModelInfo("mistral.mistral-7b-instruct-v0:2","","LLM",null));
        modelInfos.add(new ModelInfo("mistral.mistral-large-2402-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("meta.llama3-70b-instruct-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("meta.llama3-8b-instruct-v1:0","","LLM",null));
        modelInfos.add(new ModelInfo("amazon.titan-embed-text-v1","","EMBEDDING",null));
        return modelInfos;
    }


}
