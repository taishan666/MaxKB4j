package com.maxkb4j.application.pipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.LlmModelSetting;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.pipeline.step.chatstep.AbsChatStep;
import com.maxkb4j.application.service.IApplicationLongTermMemoryService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.tool.service.IToolFormatterService;
import com.maxkb4j.tool.service.IToolProviderService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStep extends AbsChatStep {

    private static final long STREAM_TIMEOUT_MINUTES = 10L;

    private static final int LONG_TERM_MEMORY_MESSAGE_LIMIT = 5;

    private final IModelProviderService modelFactory;
    private final IToolProviderService toolProvider;
    private final IToolFormatterService toolFormatterService;
    private final IApplicationLongTermMemoryService longTermMemoryService;


    @Override
    protected String execute(String chatId, String chatRecordId, ApplicationVO application, List<ChatMessage> historyMessages, String userPrompt, PipelineManage manage) throws Exception {
        Assistant assistant = buildAssistant(chatId, application, historyMessages, manage);
        StreamResult result = streamChat(assistant, userPrompt, chatId, chatRecordId, application, manage);
        context.put("reasoningContent", result.reasoning());
        recordTokenUsage(result.response());
        refreshLongTermMemory(application, manage.chatState.getChatUserId());
        return result.answer();
    }

    // ==================== Assistant 装配 ====================

    /**
     * 装配流式 {@link Assistant}：模型、系统消息（含长期记忆）、工具、聊天记忆。
     */
    private Assistant buildAssistant(String chatId, ApplicationVO application, List<ChatMessage> historyMessages, PipelineManage manage) {
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(
                application.getModelId(), application.getModelParamsSetting());
        AiServices<Assistant> builder = AiServiceFactory.builder(Assistant.class);
        applySystemMessage(builder, application, manage.chatState.getChatUserId());
        applyTools(builder, application);
        builder.chatMemory(AiChatMemory.withMessages(chatId, historyMessages));
        return builder.streamingChatModel(chatModel).build();
    }

    /**
     * 设置系统提示词；启用长期记忆时追加记忆内容到系统消息。
     */
    private void applySystemMessage(AiServices<Assistant> builder, ApplicationVO application, String chatUserId) {
        LlmModelSetting modelSetting = application.getModelSetting();
        String systemText = modelSetting == null ? null : modelSetting.getSystem();
        if (StringUtils.isNotBlank(systemText)) {
            builder.systemMessage(systemText);
        }
        if (Boolean.TRUE.equals(application.getLongTermEnable())) {
            String memory = longTermMemoryService.getMemory(application.getId(), chatUserId);
            if (StringUtils.isNotBlank(memory)) {
                builder.systemMessageTransformer(systemMessage -> systemMessage == null ? memory : systemMessage + "\n" + memory);
            }
        }
    }

    /**
     * 装配工具提供者；按需检索启用时追加知识库工具。
     * 工具装配失败时异常向上传播，由 chatMessageAsync 统一收尾，
     * 避免 emit 错误后继续执行并写入空答案记录。
     */
    private void applyTools(AiServices<Assistant> builder, ApplicationVO application) {
        List<String> toolIds = Optional.ofNullable(application.getToolIds()).orElse(List.of());
        List<String> applicationIds = Optional.ofNullable(application.getApplicationIds()).orElse(List.of());
        builder.toolProviders(toolProvider.getToolProviders(toolIds, applicationIds));
        KnowledgeSetting datasetSetting = Optional.ofNullable(application.getKnowledgeSetting()).orElseGet(KnowledgeSetting::new);
        if (Boolean.TRUE.equals(datasetSetting.getOnDemandEnable())) {
            List<String> knowledgeIds = Optional.ofNullable(application.getKnowledgeIds()).orElse(List.of());
            builder.tools(toolProvider.getKnowledgeTools(knowledgeIds, datasetSetting));
        }
    }

    // ==================== 流式对话 ====================

    /**
     * 流式执行结果。
     *
     * @param response  完整聊天响应（含 Token 用量）
     * @param answer    累积的答案文本（含工具输出）
     * @param reasoning 累积的推理内容
     */
    private record StreamResult(ChatResponse response, String answer, String reasoning) {
    }

    /**
     * 启动 Token 流并阻塞等待完成，回调期间实时向 sink 推送增量消息。
     */
    private StreamResult streamChat(Assistant assistant, String userPrompt, String chatId, String chatRecordId,
                                    ApplicationVO application, PipelineManage manage) throws Exception {
        LlmModelSetting modelSetting = application.getModelSetting();
        boolean reasoningEnable = modelSetting != null && Boolean.TRUE.equals(modelSetting.getReasoningContentEnable());
        boolean toolOutputEnable = Boolean.TRUE.equals(application.getToolOutputEnable());
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        List<String> answerTexts = new CopyOnWriteArrayList<>();
        List<String> reasoningTexts = new CopyOnWriteArrayList<>();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        TokenStream tokenStream = assistant.chatStream(userPrompt);
        // 完成后释放线程；超时置 cancelled 后各回调不再向 sink 推送
        tokenStream.onPartialThinking(thinking -> {
                    if (reasoningEnable && !cancelled.get()) {
                        emit(manage, chatId, chatRecordId, "", thinking.text());
                        reasoningTexts.add(thinking.text());
                    }
                })
                .onPartialResponse(text -> {
                    if (cancelled.get()) {
                        return;
                    }
                    emit(manage, chatId, chatRecordId, text, "");
                    answerTexts.add(text);
                })
                .beforeToolExecution(toolExecute -> {
                    if (toolOutputEnable && !cancelled.get()) {
                        emit(manage, chatId, chatRecordId, toolFormatterService.format(toolExecute), "");
                    }
                })
                .onToolExecuted(toolExecute -> {
                    if (cancelled.get() || !toolOutputEnable) {
                        return;
                    }
                    String toolText = toolFormatterService.format(toolExecute);
                    emit(manage, chatId, chatRecordId, toolText, "");
                    answerTexts.add(toolText);
                })
                .onCompleteResponse(future::complete)
                .onError(future::completeExceptionally)
                .start();
        ChatResponse response = awaitResponse(future, cancelled, chatId, chatRecordId);
        return new StreamResult(response, String.join("", answerTexts), String.join("", reasoningTexts));
    }

    /**
     * 阻塞等待流式响应；超时时置 cancelled 阻断后续回调推送。
     */
    private ChatResponse awaitResponse(CompletableFuture<ChatResponse> future, AtomicBoolean cancelled,
                                       String chatId, String chatRecordId) throws Exception {
        try {
            return future.get(STREAM_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            // 取消通知后回调不再向 sink 推送，避免超时收尾后又继续输出
            cancelled.set(true);
            log.warn("Chat stream timed out after {} minutes, chatId: {}, chatRecordId: {}", STREAM_TIMEOUT_MINUTES, chatId, chatRecordId);
            throw e;
        }
    }

    private void emit(PipelineManage manage, String chatId, String chatRecordId, String content, String reasoning) {
        manage.sink.tryEmitNext(toChatMessageVO(chatId, chatRecordId, content, reasoning, false));
    }

    // ==================== 结果落库 ====================

    private void recordTokenUsage(ChatResponse response) {
        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            context.put("messageTokens", tokenUsage.inputTokenCount());
            context.put("answerTokens", tokenUsage.outputTokenCount());
        }
    }

    /**
     * 启用长期记忆时保存本轮记忆，否则清理该应用的记忆。
     */
    private void refreshLongTermMemory(ApplicationVO application, String chatUserId) {
        String appId = application.getId();
        if (Boolean.TRUE.equals(application.getLongTermEnable())) {
            longTermMemoryService.saveMemory(appId, chatUserId, application.getModelId(), LONG_TERM_MEMORY_MESSAGE_LIMIT);
        } else {
            longTermMemoryService.deleteMemory(appId);
        }
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
