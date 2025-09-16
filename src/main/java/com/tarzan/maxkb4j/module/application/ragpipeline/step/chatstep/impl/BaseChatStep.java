package com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.MyChatMemory;
import com.tarzan.maxkb4j.core.langchain4j.MyContentRetriever;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.DatasetSetting;
import com.tarzan.maxkb4j.module.application.domian.entity.NoReferencesSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatUserStatsService;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.tarzan.maxkb4j.core.constant.PromptTemplates.RAG_PROMPT_TEMPLATE;

@Slf4j
@Component
@AllArgsConstructor
public class BaseChatStep extends IChatStep {

    private final ModelService modelService;
    private final ApplicationChatUserStatsService publicAccessClientService;
    private final ChatMemoryStore chatMemoryStore;

    @Override
    protected String execute(PipelineManage manage) {
        JSONObject context = manage.context;
        String chatId = context.getString("chatId");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraph_list");
        ApplicationVO application = (ApplicationVO) context.get("application");
        String problemText = context.getString("problem_text");
        boolean stream = true;
        return getFluxResult(chatId, paragraphList, problemText, application, manage, stream);
    }

    private String getFluxResult(String chatId,
                                 List<ParagraphVO> paragraphList,
                                 String problemText,
                                 ApplicationVO application,
                                 PipelineManage manage,
                                 boolean stream) {
        AtomicReference<String> answerText = new AtomicReference<>("");
        String chatRecordId = IdWorker.get32UUID();
        Sinks.Many<ChatMessageVO> sink = manage.sink;
        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
            if ("directly_return".equals(paragraph.getHitHandlingMethod()) && paragraph.getSimilarity() >= paragraph.getDirectlyReturnSimilarity()) {
                directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
            }
        }
        String modelId = application.getModelId();
        super.context.put("modelId", modelId);
        JSONObject params = application.getModelParamsSetting();
        DatasetSetting datasetSetting = application.getKnowledgeSetting();
        NoReferencesSetting noReferencesSetting = datasetSetting.getNoReferencesSetting();
        BaseChatModel chatModel = modelService.getModelById(modelId, params);
        if (chatModel == null) {
            answerText.set("抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。");
            sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), true));
        } else if (StringUtil.isBlank(problemText)) {
            answerText.set("用户消息不能为空");
            sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), true));
        } else {
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                answerText.set(directlyReturnChunkList.get(0).text());
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), true));
            } else if (paragraphList.isEmpty() && "designated_answer".equals(status)) {
                String value = noReferencesSetting.getValue();
                answerText.set(value.replace("{question}", problemText));
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, answerText.get(), true));
            } else {
                String chatUserId = manage.context.getString("chat_user_id");
                String chatUserType = manage.context.getString("chat_user_type");
                    int dialogueNumber = application.getDialogueNumber();
                    String systemText = application.getModelSetting().getSystem();
                    ChatMemory chatMemory = MyChatMemory.builder()
                            .id(chatId)
                            .maxMessages(dialogueNumber)
                            .chatMemoryStore(chatMemoryStore)
                            .build();
                    String system = StringUtil.isBlank(systemText) ? "You're an intelligent assistant" : systemText;
                    ContentInjector contentInjector = DefaultContentInjector.builder()
                            .promptTemplate(RAG_PROMPT_TEMPLATE)
                            // .metadataKeysToInclude(List.of("file_name", "index"))
                            .build();
                    RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                            //.queryTransformer(ExpandingQueryTransformer.builder().chatLanguageModel(chatModel.getChatModel()).build())
                            .queryRouter(new DefaultQueryRouter(new MyContentRetriever(paragraphList)))
                            .contentAggregator(new DefaultContentAggregator())
                            .contentInjector(contentInjector)
                            .build();
                    Assistant assistant = AiServices.builder(Assistant.class)
                            .systemMessageProvider(chatMemoryId -> system)
                            .chatMemory(chatMemory)
                            .streamingChatModel(chatModel.getStreamingChatModel())
                            .retrievalAugmentor(retrievalAugmentor)
                            .build();
                    if (stream) {
                        TokenStream tokenStream = assistant.chatStream(problemText);
                        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
                        tokenStream.onToolExecuted((ToolExecution toolExecution) -> log.info("toolExecution={}", toolExecution))
                                .onPartialThinking(thinking-> sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "",thinking.text(), false)))
                                .onPartialResponse(text -> sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text,"", false)))
                                .onCompleteResponse(response -> {
                                    answerText.set(response.aiMessage().text());
                                    TokenUsage tokenUsage = response.tokenUsage();
                                    context.put("message_list", chatMemory.messages());
                                    context.put("messageTokens", tokenUsage.inputTokenCount());
                                    context.put("answerTokens", tokenUsage.outputTokenCount());
                                    addAccessNum(chatUserId, chatUserType);
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", true, tokenUsage.inputTokenCount(), tokenUsage.outputTokenCount()));
                                    futureChatResponse.complete(response);// 完成后释放线程
                                })
                                .onError(error -> {
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", true));
                                    futureChatResponse.completeExceptionally(error); // 完成后释放线程
                                })
                                .start();
                        futureChatResponse.join(); // 阻塞当前线程直到 futureChatResponse 完成
                    }
                long startTime = manage.context.getLong("start_time");
            }
        }
        return answerText.get();
    }


    public JSONArray resetMessageList(JSONArray messageList, String answerText) {
        if (CollectionUtils.isEmpty(messageList)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (Object o : messageList) {
            JSONObject message = new JSONObject();
            if (o instanceof SystemMessage systemMessage) {
                message.put("role", "system");
                message.put("content", systemMessage.text());
            }
            if (o instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
            }
            if (o instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
            }
            newMessageList.add(message);
        }
        JSONObject aiMessage = new JSONObject();
        aiMessage.put("role", "ai");
        aiMessage.put("content", answerText);
        newMessageList.add(aiMessage);
        return newMessageList;
    }


    @Override
    public JSONObject getDetails() {
        long startTime = context.getLong("start_time");
        JSONObject details = new JSONObject();
        details.put("step_type", "chat_step");
        details.put("runTime", (System.currentTimeMillis() - startTime) / 1000F);
        details.put("modelId", context.get("modelId"));
        //todo message_list
        details.put("message_list", resetMessageList(context.getJSONArray("message_list"), context.getString("answer_text")));
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
