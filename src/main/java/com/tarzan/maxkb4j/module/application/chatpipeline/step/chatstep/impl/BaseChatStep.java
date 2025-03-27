package com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.assistant.Assistant;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.rag.MyChatMemory;
import com.tarzan.maxkb4j.module.rag.MyContentRetriever;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.AllArgsConstructor;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class BaseChatStep extends IChatStep {

    private final ModelService modelService;
    private final ApplicationPublicAccessClientService publicAccessClientService;
    @Override
    protected Flux<ChatMessageVO> execute(PipelineManage manage) {
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
       // List<ChatMessage> messageList = (List<ChatMessage>) context.get("message_list");
        int dialogueNumber = application.getDialogueNumber();
        List<ApplicationChatRecordEntity> chatRecordList = (List<ApplicationChatRecordEntity>) context.get("chatRecordList");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraph_list");
        String modelId = application.getModelId();
        super.context.put("modelId", modelId);
        PostResponseHandler postResponseHandler = (PostResponseHandler) context.get("postResponseHandler");
        String problemText = context.getString("problem_text");
        JSONObject params=application.getModelParamsSetting();
        String systemText = application.getModelSetting().getSystem();
        DatasetSetting datasetSetting = application.getDatasetSetting();
        NoReferencesSetting noReferencesSetting = datasetSetting.getNoReferencesSetting();
        String chatId = context.getString("chatId");
        BaseChatModel chatModel = modelService.getModelById(modelId, params);
        boolean stream = true;
        return getFluxResult(chatId, chatRecordList,dialogueNumber, chatModel, paragraphList, noReferencesSetting,systemText, problemText, manage, postResponseHandler, stream);
    }


    private Flux<ChatMessageVO> getFluxResult(String chatId, List<ApplicationChatRecordEntity> chatRecordList,int dialogueNumber,
                                           BaseChatModel chatModel,
                                           List<ParagraphVO> paragraphList,
                                           NoReferencesSetting noReferencesSetting,
                                           String systemText,String problemText, PipelineManage manage, PostResponseHandler postResponseHandler, boolean stream) {
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
            sink.tryEmitNext(new ChatMessageVO(chatId,chatRecordId,text,true));
        } else {
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                String text = directlyReturnChunkList.get(0).text();
                sink.tryEmitNext(new ChatMessageVO(chatId,chatRecordId,text,true));
                sink.tryEmitComplete();
            } else if (paragraphList.isEmpty()&&"designated_answer".equals(status)) {
                    String value = noReferencesSetting.getValue();
                    String text = value.replace("{question}", problemText);
                    sink.tryEmitNext(new ChatMessageVO(chatId,chatRecordId,text,true));
                    sink.tryEmitComplete();
            } else {
                int messageTokens = manage.context.getInteger("messageTokens");
                int answerTokens = manage.context.getInteger("answerTokens");
                String clientId = manage.context.getString("client_id");
                String clientType = manage.context.getString("client_type");
                MyChatMemory chatMemory=new MyChatMemory(chatRecordList,dialogueNumber);
                String system= StringUtil.isBlank(systemText)?"You're an intelligent assistant":systemText;
                Assistant assistant =  AiServices.builder(Assistant.class)
                        .systemMessageProvider(chatMemoryId ->system)
                    //    .chatLanguageModel(chatModel.getChatModel())
                        .streamingChatLanguageModel(chatModel.getStreamingChatModel())
                        .chatMemory(chatMemory)
                        .contentRetriever(new MyContentRetriever(paragraphList))
                        .build();
                if (stream) {
                    TokenStream tokenStream = assistant.chatStream(problemText);
                    tokenStream.onPartialResponse(text -> sink.tryEmitNext(new ChatMessageVO(chatId,chatRecordId,text,false)))
                            .onCompleteResponse(response->{
                                String  answerText=response.aiMessage().text();
                                TokenUsage tokenUsage=response.tokenUsage();
                                int thisMessageTokens = tokenUsage.inputTokenCount();
                                int thisAnswerTokens = tokenUsage.outputTokenCount();
                                manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                                manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                                addAccessNum(clientId, clientType);
                                postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);
                                sink.tryEmitNext(new ChatMessageVO(chatId,chatRecordId,"",true));
                                sink.tryEmitComplete();
                            })
                            .onError(error->{
                                sink.tryEmitNext(new ChatMessageVO(chatId,chatRecordId,"",true));
                                sink.tryEmitComplete();
                            })
                            .start();
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
