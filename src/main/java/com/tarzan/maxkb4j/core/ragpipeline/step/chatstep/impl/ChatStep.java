package com.tarzan.maxkb4j.core.ragpipeline.step.chatstep.impl;

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.entity.NoReferencesSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.AIAnswerType;
import com.tarzan.maxkb4j.core.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.ragpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatUserStatsService;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class ChatStep extends IChatStep {

    private final ModelService modelService;
    private final ApplicationChatUserStatsService publicAccessClientService;
    private final AiServices<Assistant> aiServicesBuilder;

    public ChatStep(ModelService modelService,
                    ApplicationChatUserStatsService publicAccessClientService,
                    ChatMemoryStore chatMemoryStore) {
        this.modelService = modelService;
        this.publicAccessClientService = publicAccessClientService;
        this.aiServicesBuilder = AiServices.builder(Assistant.class);
    }

    @Override
    protected String execute(PipelineManage manage) {
        String chatId = manage.context.getString("chatId");
        @SuppressWarnings("unchecked")
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) manage.context.get("paragraphList");
        ApplicationVO application = manage.context.getObject("application", ApplicationVO.class);
        String userPrompt = manage.context.getString("user_prompt");
        boolean stream = true;
        return getFluxResult(chatId, paragraphList, userPrompt, application, manage, stream);
    }

    private String getFluxResult(String chatId,
                                 List<ParagraphVO> paragraphList,
                                 String userPrompt,
                                 ApplicationVO application,
                                 PipelineManage manage,
                                 boolean stream) {
        AtomicReference<String> answerText = new AtomicReference<>("");
        String chatRecordId = manage.context.getString("chatRecordId");
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
        //super.context.put("modelId", modelId);
        JSONObject params = application.getModelParamsSetting();
        KnowledgeSetting knowledgeSetting = application.getKnowledgeSetting();
        NoReferencesSetting noReferencesSetting = knowledgeSetting.getNoReferencesSetting();
        BaseChatModel chatModel = modelService.getModelById(modelId, params);
        String problemText = manage.context.getString("problem_text");
        if (chatModel == null) {
            answerText.set("抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。");
            sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), "", "ai-chat-node", viewType, true));
        } else if (StringUtil.isBlank(problemText)) {
            answerText.set("用户消息不能为空");
            sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), "", "ai-chat-node", viewType, true));
        } else {
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                answerText.set(directlyReturnChunkList.get(0).text());
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), "", "ai-chat-node", viewType, true));
            } else if (paragraphList.isEmpty() && AIAnswerType.designated_answer.name().equals(status)) {
                String value = noReferencesSetting.getValue();
                answerText.set(value.replace("{question}", problemText));
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), "", "ai-chat-node", viewType, true));
            } else {
                String chatUserId = manage.context.getString("chat_user_id");
                String chatUserType = manage.context.getString("chat_user_type");
                String systemText = application.getModelSetting().getSystem();
                if (StringUtils.isNotBlank(systemText)){
                    aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemText);
                }
                int dialogueNumber = application.getDialogueNumber();
                List<ChatMessage> historyMessages=manage.getHistoryMessages(dialogueNumber);
                Assistant assistant =  aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages)).streamingChatModel(chatModel.getStreamingChatModel()).build();
                if (stream) {
                    boolean reasoningEnable = application.getModelSetting().getReasoningContentEnable();
                    TokenStream tokenStream = assistant.chatStream(userPrompt);
                    CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
                    tokenStream.onPartialThinking(thinking -> {
                                if (reasoningEnable) {
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", thinking.text(), "ai-chat-node", viewType, false));
                                }
                            })
                            .onPartialResponse(text -> sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, "", "ai-chat-node", viewType, false)))
                            .onCompleteResponse(response -> {
                                answerText.set(response.aiMessage().text());
                                TokenUsage tokenUsage = response.tokenUsage();
                                context.put("message_list", resetMessageList(historyMessages));
                                context.put("messageTokens", tokenUsage.inputTokenCount());
                                context.put("answerTokens", tokenUsage.outputTokenCount());
                                addAccessNum(chatUserId, chatUserType);
                                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", "", "ai-chat-node", viewType, true));
                                futureChatResponse.complete(response);// 完成后释放线程
                            })
                            .onError(error -> {
                                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", "", "ai-chat-node", viewType, true));
                                futureChatResponse.completeExceptionally(error); // 完成后释放线程
                            })
                            .start();
                    futureChatResponse.join(); // 阻塞当前线程直到 futureChatResponse 完成
                }
            }
        }
        return answerText.get();
    }


    public JSONArray resetMessageList(List<ChatMessage> historyMessages) {
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
        long startTime = context.getLong("start_time");
        JSONObject details = new JSONObject();
        details.put("step_type", "chat_step");
        details.put("runTime", (System.currentTimeMillis() - startTime) / 1000F);
        //details.put("modelId", context.get("modelId"));
        //todo message_list
        details.put("message_list", context.get("message_list"));
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        details.put("cost", 0);
        return details;
    }


    private void addAccessNum(String clientId, String clientType) {
        if ("APPLICATION_ACCESS_TOKEN".equals(clientType)) {
            ApplicationChatUserStatsEntity publicAccessClient = publicAccessClientService.getById(clientId);
            if (publicAccessClient != null) {
                publicAccessClient.setAccessNum(publicAccessClient.getAccessNum() + 1);
                publicAccessClient.setIntraDayAccessNum(publicAccessClient.getIntraDayAccessNum() + 1);
                publicAccessClientService.updateById(publicAccessClient);
            }
        }
    }

}
