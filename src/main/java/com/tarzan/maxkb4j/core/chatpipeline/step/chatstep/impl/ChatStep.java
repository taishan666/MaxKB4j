package com.tarzan.maxkb4j.core.chatpipeline.step.chatstep.impl;

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.entity.NoReferencesSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.AIAnswerType;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@AllArgsConstructor
public class ChatStep extends IChatStep {

    private final ModelFactory modelFactory;
    private final ToolUtil toolUtil;

    @Override
    protected String execute(PipelineManage manage) {
        String chatId = (String) manage.context.get("chatId");
        @SuppressWarnings("unchecked")
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) manage.context.get("paragraphList");
        ApplicationVO application = (ApplicationVO) manage.context.get("application");
        String userPrompt = (String) manage.context.get("user_prompt");
        return getFluxResult(chatId, paragraphList, userPrompt, application, manage);
    }

    private String getFluxResult(String chatId,
                                 List<ParagraphVO> paragraphList,
                                 String userPrompt,
                                 ApplicationVO application,
                                 PipelineManage manage) {
        AtomicReference<String> answerText = new AtomicReference<>("");
        String chatRecordId = (String) manage.context.get("chatRecordId");
        Sinks.Many<ChatMessageVO> sink = manage.sink;
        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
            if ("directlyReturn".equals(paragraph.getHitHandlingMethod()) && paragraph.getSimilarity() >= paragraph.getDirectlyReturnSimilarity()) {
                directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
            }
        }
        String modelId = application.getModelId();
        JSONObject params = application.getModelParamsSetting();
        KnowledgeSetting knowledgeSetting = application.getKnowledgeSetting();
        NoReferencesSetting noReferencesSetting = knowledgeSetting.getNoReferencesSetting();
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId, params);
        String problemText = (String) manage.context.get("problemText");
        if (chatModel == null) {
            answerText.set("抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。");
        } else if (StringUtils.isBlank(problemText)) {
            answerText.set("用户消息不能为空");
        } else {
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                answerText.set(directlyReturnChunkList.get(0).text());
            } else if (paragraphList.isEmpty() && AIAnswerType.designated_answer.name().equals(status)) {
                String value = noReferencesSetting.getValue();
                answerText.set(value.replace("{question}", problemText));
            } else {
                String systemText = application.getModelSetting().getSystem();
                AiServices<Assistant> aiServicesBuilder=AiServices.builder(Assistant.class);
                if (StringUtils.isNotBlank(systemText)){
                    aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemText);
                }
                if (!CollectionUtils.isEmpty(application.getToolIds())) {
                    aiServicesBuilder.tools(toolUtil.getToolMap(application.getToolIds()));
                }
                int dialogueNumber = application.getDialogueNumber();
                List<ChatMessage> historyMessages=manage.getHistoryMessages(dialogueNumber);
                Assistant assistant =  aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages)).streamingChatModel(chatModel).build();
                Boolean reasoningEnable = application.getModelSetting().getReasoningContentEnable();
                TokenStream tokenStream = assistant.chatStream(userPrompt);
                CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
                tokenStream.onPartialThinking(thinking -> {
                            if (Boolean.TRUE.equals(reasoningEnable)) {
                                sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, "", thinking.text(), false));
                            }
                        })
                        .onPartialResponse(text -> sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, text, "",  false)))
                        .onToolExecuted(toolExecute -> {
                            if (Boolean.TRUE.equals(application.getToolOutputEnable())){
                                sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId,  MessageTools.getToolMessage(toolExecute), "",  false));
                            }
                        })
                        .onCompleteResponse(response -> {
                            answerText.set(response.aiMessage().text());
                            TokenUsage tokenUsage = response.tokenUsage();
                            context.put("messageList", resetMessageToJSON(historyMessages));
                            context.put("messageTokens", tokenUsage.inputTokenCount());
                            context.put("answerTokens", tokenUsage.outputTokenCount());
                            futureChatResponse.complete(response);// 完成后释放线程
                        })
                        .onError(error -> {
                            log.error("执行错误", error);
                            futureChatResponse.completeExceptionally(error); // 完成后释放线程
                        })
                        .start();
                futureChatResponse.join(); // 阻塞当前线程直到 futureChatResponse 完成
                sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, "", "", true));
                return answerText.get();
            }
        }
        sink.tryEmitNext(super.toChatMessageVO(chatId, chatRecordId, answerText.get(), "",true));
        return answerText.get();
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
        JSONObject details = new JSONObject();
        details.put("step_type", "chat_step");
        details.put("messageList", context.get("messageList"));
        details.put("runTime", context.get("runTime"));
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }




}
