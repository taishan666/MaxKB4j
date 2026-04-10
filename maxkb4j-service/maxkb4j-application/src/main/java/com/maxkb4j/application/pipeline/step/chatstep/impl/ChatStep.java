package com.maxkb4j.application.pipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.pipeline.step.chatstep.AbsChatStep;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AppChatMemory;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.tool.service.IToolProviderService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStep extends AbsChatStep {

    private final IModelProviderService modelFactory;
    private final IToolProviderService toolProvider;


    @Override
    protected String execute(String chatId, String chatRecordId, ApplicationVO application, String userPrompt, PipelineManage manage) {
        List<String> answerTexts = new ArrayList<>();
        String modelId = application.getModelId();
        JSONObject params = application.getModelParamsSetting();
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId, params);
        String systemText = application.getModelSetting().getSystem();
        List<String> toolIds = Optional.ofNullable(application.getToolIds()).orElse(List.of());
        List<String> applicationIds = Optional.ofNullable(application.getApplicationIds()).orElse(List.of());
        AiServices<Assistant> aiServicesBuilder = AssistantServices.builder(Assistant.class);
        if (StringUtils.isNotBlank(systemText)){
            aiServicesBuilder.systemMessage(systemText);
        }
        try {
            aiServicesBuilder.toolProvider(toolProvider.getSkillsProvider(modelId, toolIds));
            aiServicesBuilder.tools(toolProvider.getToolMap(toolIds, applicationIds));
        }catch (ApiException e){
            manage.sink.tryEmitError(e);
        }
        int dialogueNumber = application.getDialogueNumber();
        List<ChatMessage> historyMessages = manage.getHistoryMessages(dialogueNumber);
        Assistant assistant = aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages)).streamingChatModel(chatModel).build();
        Boolean reasoningEnable = application.getModelSetting().getReasoningContentEnable();
        TokenStream tokenStream = assistant.chatStream(userPrompt);
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
        tokenStream.onPartialThinking(thinking -> {
                    if (Boolean.TRUE.equals(reasoningEnable)) {
                        manage.sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, "", thinking.text(), false));
                    }
                })
                .onPartialResponse(text -> {
                    manage.sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, text, "", false));
                    answerTexts.add(text);
                })
                .beforeToolExecution(toolExecute -> {
                    if (Boolean.TRUE.equals(application.getToolOutputEnable())) {
                        manage.sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, toolProvider.format(toolExecute), "", false));
                    }
                })
                .onToolExecuted(toolExecute -> {
                    if (Boolean.TRUE.equals(application.getToolOutputEnable())) {
                        String toolText = toolProvider.format(toolExecute);
                        manage.sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, toolText, "", false));
                        answerTexts.add(toolText);
                    }
                })
                .onCompleteResponse(response -> {
                    TokenUsage tokenUsage = response.tokenUsage();
                    context.put("messageTokens", tokenUsage.inputTokenCount());
                    context.put("answerTokens", tokenUsage.outputTokenCount());
                    futureChatResponse.complete(response);// 完成后释放线程
                })
                .onError(error -> {
                    log.error("执行错误", error);
                    futureChatResponse.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        ChatResponse response = futureChatResponse.join();
        context.put("messageList", resetMessageToJSON(historyMessages));
        context.put("reasoningContent", response.aiMessage().thinking());
        return String.join("", answerTexts);
    }


    public JSONArray resetMessageToJSON(List<ChatMessage> historyMessages) {
        if (CollectionUtils.isEmpty(historyMessages)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessages) {
            JSONObject message = new JSONObject();
            if (chatMessage instanceof SystemMessage systemMessage) {
                message.put("role", "system");
                message.put("content", systemMessage.text());
            }
            if (chatMessage instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
            }
            if (chatMessage instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
            }
            newMessageList.add(message);
        }
        return newMessageList;
    }


    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject(true);
        details.put("step_type", "chat_step");
        details.put("messageList", context.get("messageList"));
        details.put("runTime", context.get("runTime"));
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }


}
