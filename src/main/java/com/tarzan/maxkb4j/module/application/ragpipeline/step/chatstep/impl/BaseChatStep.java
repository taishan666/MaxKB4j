package com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.ragpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.assistant.Assistant;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.mcplib.entity.McpLibEntity;
import com.tarzan.maxkb4j.module.mcplib.service.McpLibService;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.rag.MyChatMemory;
import com.tarzan.maxkb4j.module.rag.MyContentRetriever;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BaseChatStep extends IChatStep {

    private final ModelService modelService;
    private final McpLibService mcpLibService;
    private final ApplicationPublicAccessClientService publicAccessClientService;

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                    {{userMessage}}
                    
                    Use the contents of the <Data></Data> tag as your knowledge:
                    <Data>
                     {{contents}}
                    </Data>
                    Answer the request:
                    
                    - If you don't know, just say you don't know. If you don't know when in doubt, seek clarification.
                    
                    - Avoid mentioning information that you get from context.
                    
                    - Answer in the same language as the question.
                    """);

    @Override
    protected Flux<ChatMessageVO> execute(PipelineManage manage) {
        JSONObject context = manage.context;
        String chatId = context.getString("chatId");
        List<ApplicationChatRecordEntity> chatRecordList = (List<ApplicationChatRecordEntity>) context.get("chatRecordList");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraph_list");
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        String problemText = context.getString("problem_text");
        PostResponseHandler postResponseHandler = (PostResponseHandler) context.get("postResponseHandler");
   /*     int dialogueNumber = application.getDialogueNumber();
        String modelId = application.getModelId();
        super.context.put("modelId", modelId);

        JSONObject params=application.getModelParamsSetting();
        String systemText = application.getModelSetting().getSystem();
        DatasetSetting datasetSetting = application.getDatasetSetting();
        NoReferencesSetting noReferencesSetting = datasetSetting.getNoReferencesSetting();
        BaseChatModel chatModel = modelService.getModelById(modelId, params);*/
        boolean stream = true;
        //   return getFluxResult(chatId, chatRecordList,dialogueNumber, chatModel, paragraphList, noReferencesSetting,systemText, problemText, manage, postResponseHandler, stream);
        return getFluxResult1(chatId, chatRecordList, paragraphList, problemText, application, manage, postResponseHandler, stream);
    }

    private Flux<ChatMessageVO> getFluxResult1(String chatId,
                                               List<ApplicationChatRecordEntity> chatRecordList,
                                               List<ParagraphVO> paragraphList,
                                               String problemText,
                                               ApplicationEntity application,
                                               PipelineManage manage,
                                               PostResponseHandler postResponseHandler,
                                               boolean stream) {
        long startTime = System.currentTimeMillis();
        String chatRecordId = IdWorker.get32UUID();
        Sinks.Many<ChatMessageVO> sink = Sinks.many().multicast().onBackpressureBuffer();
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
        DatasetSetting datasetSetting = application.getDatasetSetting();
        NoReferencesSetting noReferencesSetting = datasetSetting.getNoReferencesSetting();
        BaseChatModel chatModel = modelService.getModelById(modelId, params);
        if (chatModel == null) {
            String text = "抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。";
            sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, true));
        } else {
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                String text = directlyReturnChunkList.get(0).text();
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, true));
                sink.tryEmitComplete();
            } else if (paragraphList.isEmpty() && "designated_answer".equals(status)) {
                String value = noReferencesSetting.getValue();
                String text = value.replace("{question}", problemText);
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, true));
                sink.tryEmitComplete();
            } else {
                int dialogueNumber = application.getDialogueNumber();
                String systemText = application.getModelSetting().getSystem();
                int messageTokens = manage.context.getInteger("messageTokens");
                int answerTokens = manage.context.getInteger("answerTokens");
                String clientId = manage.context.getString("client_id");
                String clientType = manage.context.getString("client_type");
                MyChatMemory chatMemory = new MyChatMemory(chatRecordList, dialogueNumber);
                String system = StringUtil.isBlank(systemText) ? "You're an intelligent assistant" : systemText;
                ContentInjector contentInjector = DefaultContentInjector.builder()
                        .promptTemplate(DEFAULT_PROMPT_TEMPLATE)
                        .metadataKeysToInclude(List.of("file_name", "index"))
                        .build();
                RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                        //.queryTransformer(ExpandingQueryTransformer.builder().chatLanguageModel(chatModel.getChatModel()).build())
                        //  .contentRetriever(new MyContentRetriever(paragraphList))
                        .queryRouter(new DefaultQueryRouter(new MyContentRetriever(paragraphList)))
                        .contentAggregator(new DefaultContentAggregator())
                        .contentInjector(contentInjector)
                        .build();
        /*        McpClient mcpClient = new DefaultMcpClient.Builder()
                        .transport(new HttpMcpTransport.Builder().sseUrl("").build())
                        .build();
                McpClient mcpClient1 = new DefaultMcpClient.Builder()
                        .transport(new HttpMcpTransport.Builder().sseUrl("").build())
                        .build();*/
                List<String> mcpIds = application.getMcpIdList();
                List<McpClient> mcpClients = new ArrayList<>();
                if (!CollectionUtils.isEmpty(mcpIds)){
                   List<McpLibEntity> mcpLib= mcpLibService.listByIds(mcpIds);
                    for (McpLibEntity mcpLibEntity : mcpLib) {
                        McpClient mcpClient = new DefaultMcpClient.Builder()
                                .transport(new HttpMcpTransport.Builder().sseUrl(mcpLibEntity.getSseUrl()).build())
                                .build();
                        mcpClients.add(mcpClient);
                    }
                }
                ToolProvider toolProvider = McpToolProvider.builder()
                        .mcpClients(mcpClients)
                        .build();
                Assistant assistant = AiServices.builder(Assistant.class)
                        .systemMessageProvider(chatMemoryId -> system)
                        .streamingChatLanguageModel(chatModel.getStreamingChatModel())
                        .retrievalAugmentor(retrievalAugmentor)
                        .chatMemory(chatMemory)
                        .toolProvider(toolProvider)
                        .build();
                if (stream) {
                    if (StringUtil.isBlank(problemText)) {
                        manage.context.put("messageTokens", messageTokens);
                        manage.context.put("answerTokens", answerTokens);
                        sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "用户消息不能为空", true));
                        sink.tryEmitComplete();
                    } else {
                        TokenStream tokenStream = assistant.chatStream(problemText);
                        tokenStream.onPartialResponse(text -> sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, false)))
                                .onCompleteResponse(response -> {
                                    String answerText = response.aiMessage().text();
                                    TokenUsage tokenUsage = response.tokenUsage();
                                    int thisMessageTokens = tokenUsage.inputTokenCount();
                                    int thisAnswerTokens = tokenUsage.outputTokenCount();
                                    manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                                    manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                                    addAccessNum(clientId, clientType);
                                    postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", true));
                                    sink.tryEmitComplete();
                                })
                                .onError(error -> {
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", true));
                                    sink.tryEmitComplete();
                                })
                                .start();
                        log.info("AI回答 耗时：{} ms", System.currentTimeMillis() - startTime);
                    }
                } else {
              /*      String response = assistant.chat(problemText);
                    System.out.println(response);
                    String  answerText=response;
                    TokenUsage tokenUsage=new TokenUsage();
                    sink.tryEmitNext(toResponse(chatId, chatRecordId, answerText, true, 0, 0));
                    sink.tryEmitComplete();
                    int thisMessageTokens = tokenUsage.inputTokenCount();
                    int thisAnswerTokens = tokenUsage.outputTokenCount();
                    manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                    manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                    addAccessNum(clientId, clientType);
                    postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);*/
                }
            }
        }
        return sink.asFlux();
    }

    private Flux<ChatMessageVO> getFluxResult(String chatId, List<ApplicationChatRecordEntity> chatRecordList, int dialogueNumber,
                                              BaseChatModel chatModel,
                                              List<ParagraphVO> paragraphList,
                                              NoReferencesSetting noReferencesSetting,
                                              String systemText, String problemText, PipelineManage manage, PostResponseHandler postResponseHandler, boolean stream) {
        long startTime = System.currentTimeMillis();
        String chatRecordId = IdWorker.get32UUID();
        Sinks.Many<ChatMessageVO> sink = Sinks.many().multicast().onBackpressureBuffer();
        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
            if ("directly_return".equals(paragraph.getHitHandlingMethod()) && paragraph.getSimilarity() >= paragraph.getDirectlyReturnSimilarity()) {
                directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
            }
        }
        if (chatModel == null) {
            String text = "抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。";
            sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, true));
        } else {
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                String text = directlyReturnChunkList.get(0).text();
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, true));
                sink.tryEmitComplete();
            } else if (paragraphList.isEmpty() && "designated_answer".equals(status)) {
                String value = noReferencesSetting.getValue();
                String text = value.replace("{question}", problemText);
                sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, true));
                sink.tryEmitComplete();
            } else {
                int messageTokens = manage.context.getInteger("messageTokens");
                int answerTokens = manage.context.getInteger("answerTokens");
                String clientId = manage.context.getString("client_id");
                String clientType = manage.context.getString("client_type");
                MyChatMemory chatMemory = new MyChatMemory(chatRecordList, dialogueNumber);
                String system = StringUtil.isBlank(systemText) ? "You're an intelligent assistant" : systemText;
                ContentInjector contentInjector = DefaultContentInjector.builder()
                        .promptTemplate(DEFAULT_PROMPT_TEMPLATE)
                        .metadataKeysToInclude(List.of("file_name", "index"))
                        .build();
                RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                        //.queryTransformer(ExpandingQueryTransformer.builder().chatLanguageModel(chatModel.getChatModel()).build())
                        //  .contentRetriever(new MyContentRetriever(paragraphList))
                        .queryRouter(new DefaultQueryRouter(new MyContentRetriever(paragraphList)))
                        .contentAggregator(new DefaultContentAggregator())
                        .contentInjector(contentInjector)
                        .build();
        /*        McpClient mcpClient = new DefaultMcpClient.Builder()
                        .transport(new HttpMcpTransport.Builder().sseUrl("").build())
                        .build();
                McpClient mcpClient1 = new DefaultMcpClient.Builder()
                        .transport(new HttpMcpTransport.Builder().sseUrl("").build())
                        .build();*/
                ToolProvider toolProvider = McpToolProvider.builder()
                        .mcpClients(List.of())
                        .build();
                Assistant assistant = AiServices.builder(Assistant.class)
                        .systemMessageProvider(chatMemoryId -> system)
                        .streamingChatLanguageModel(chatModel.getStreamingChatModel())
                        .retrievalAugmentor(retrievalAugmentor)
                        .chatMemory(chatMemory)
                        .toolProvider(toolProvider)
                        .build();
                if (stream) {
                    if (StringUtil.isBlank(problemText)) {
                        manage.context.put("messageTokens", messageTokens);
                        manage.context.put("answerTokens", answerTokens);
                        sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "用户消息不能为空", true));
                        sink.tryEmitComplete();
                    } else {
                        TokenStream tokenStream = assistant.chatStream(problemText);
                        tokenStream.onPartialResponse(text -> sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, text, false)))
                                .onCompleteResponse(response -> {
                                    String answerText = response.aiMessage().text();
                                    TokenUsage tokenUsage = response.tokenUsage();
                                    int thisMessageTokens = tokenUsage.inputTokenCount();
                                    int thisAnswerTokens = tokenUsage.outputTokenCount();
                                    manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                                    manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                                    addAccessNum(clientId, clientType);
                                    postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", true));
                                    sink.tryEmitComplete();
                                })
                                .onError(error -> {
                                    sink.tryEmitNext(new ChatMessageVO(chatId, chatRecordId, "", true));
                                    sink.tryEmitComplete();
                                })
                                .start();
                        log.info("AI回答 耗时：{} ms", System.currentTimeMillis() - startTime);
                    }
                } else {
              /*      String response = assistant.chat(problemText);
                    System.out.println(response);
                    String  answerText=response;
                    TokenUsage tokenUsage=new TokenUsage();
                    sink.tryEmitNext(toResponse(chatId, chatRecordId, answerText, true, 0, 0));
                    sink.tryEmitComplete();
                    int thisMessageTokens = tokenUsage.inputTokenCount();
                    int thisAnswerTokens = tokenUsage.outputTokenCount();
                    manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                    manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                    addAccessNum(clientId, clientType);
                    postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);*/
                }
            }
        }
        return sink.asFlux();
    }

    public JSONArray resetMessageList(JSONArray messageList, String answerText) {
        if (CollectionUtils.isEmpty(messageList)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (Object o : messageList) {
            JSONObject message = new JSONObject();
            if (o instanceof SystemMessage systemMessage) {
                message.put("role", "ai");
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
        long startTime = super.context.getLong("start_time");
        JSONObject details = new JSONObject();
        details.put("step_type", "chat_step");
        details.put("runTime", (System.currentTimeMillis() - startTime) / 1000F);
        details.put("modelId", super.context.get("modelId"));
        details.put("message_list", resetMessageList(super.context.getJSONArray("message_list"), super.context.getString("answer_text")));
        details.put("messageTokens", super.context.get("messageTokens"));
        details.put("answerTokens", super.context.get("answerTokens"));
        details.put("cost", 0);
        return details;
    }


    private void addAccessNum(String clientId, String clientType) {
        if ("APPLICATION_ACCESS_TOKEN".equals(clientType)) {
            ApplicationPublicAccessClientEntity publicAccessClient = publicAccessClientService.getById(clientId);
            if (publicAccessClient != null) {
                publicAccessClient.setAccessNum(publicAccessClient.getAccessNum() + 1);
                publicAccessClient.setIntraDayAccessNum(publicAccessClient.getIntraDayAccessNum() + 1);
                publicAccessClientService.updateById(publicAccessClient);
            }
        }
    }

}
