package com.tarzan.maxkb4j.module.chatpipeline.chatstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseChatStep extends IChatStep {


    @Override
    protected Map<String, Object> execute(PipelineManage manage) throws Exception {
        System.out.println("BaseChatStep: "+manage.getContext());
        JSONObject context = manage.getContext();
        ApplicationEntity application = context.getJSONObject("application").toJavaObject(ApplicationEntity.class);
        List<ChatMessage> messages =(List<ChatMessage>) context.get("messageList");
        UUID modelId = application.getModelId();
        ModelService modelService= SpringUtil.getBean(ModelService.class);
        ChatLanguageModel chatModel=modelService.getChatModelById(modelId);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .build();
        ChatResponse chatResponse = chatModel.chat(chatRequest);
        System.out.println("chatResponse: "+chatResponse);
        return null;
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of();
    }
}
