package com.tarzan.maxkb4j.module.modelprovider.awsbedrockmodelprovider;

import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.util.IoUtil;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_aws_bedrock_provider")
public class AwsBedrockModelProvider  implements IModelProvider {
    @Override
    public ModelProvideInfo getModelProvideInfo() {
        ModelProvideInfo info = new ModelProvideInfo();
        info.setProvider("model_aws_bedrock_provider");
        info.setName("Amazon Bedrock");
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/bedrock_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("anthropic.claude-v2:1","","LLM"));
        modelInfos.add(new ModelInfo("anthropic.claude-v2","","LLM"));
        modelInfos.add(new ModelInfo("anthropic.claude-3-haiku-20240307-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("anthropic.claude-3-sonnet-20240229-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("anthropic.claude-3-5-sonnet-20240620-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("anthropic.claude-instant-v1","","LLM"));
        modelInfos.add(new ModelInfo("amazon.titan-text-premier-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("amazon.titan-text-lite-v1","","LLM"));
        modelInfos.add(new ModelInfo("amazon.titan-text-express-v1","","LLM"));
        modelInfos.add(new ModelInfo("mistral.mistral-7b-instruct-v0:2","","LLM"));
        modelInfos.add(new ModelInfo("mistral.mistral-large-2402-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("meta.llama3-70b-instruct-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("meta.llama3-8b-instruct-v1:0","","LLM"));
        modelInfos.add(new ModelInfo("amazon.titan-embed-text-v1","","EMBEDDING"));
        return modelInfos;
    }
}
