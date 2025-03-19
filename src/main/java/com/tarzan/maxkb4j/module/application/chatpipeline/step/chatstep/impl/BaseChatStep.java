package com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.entity.DatasetSetting;
import com.tarzan.maxkb4j.module.application.entity.NoReferencesSetting;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.module.assistant.Assistant;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
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
import lombok.AllArgsConstructor;
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
    protected Flux<JSONObject> execute(PipelineManage manage) {
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        List<ChatMessage> messageList = (List<ChatMessage>) context.get("message_list");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraph_list");
        String modelId = application.getModelId();
        super.context.put("model_id", modelId);
        PostResponseHandler postResponseHandler = (PostResponseHandler) context.get("postResponseHandler");
        String problemText = context.getString("problem_text");
        DatasetSetting datasetSetting = application.getDatasetSetting();
        NoReferencesSetting noReferencesSetting = datasetSetting.getNoReferencesSetting();
        String chatId = context.getString("chatId");
        String chatRecordId = IdWorker.get32UUID();
        BaseChatModel chatModel = modelService.getModelById(modelId);
        boolean stream = true;
        return getFluxResult(chatId, chatRecordId, messageList, chatModel, paragraphList, noReferencesSetting, problemText, manage, postResponseHandler, stream);
    }


    private Flux<JSONObject> getFluxResult(String chatId, String chatRecordId, List<ChatMessage> messageList,
                                           BaseChatModel chatModel,
                                           List<ParagraphVO> paragraphList,
                                           NoReferencesSetting noReferencesSetting,
                                           String problemText, PipelineManage manage, PostResponseHandler postResponseHandler, boolean stream) {
        Sinks.Many<JSONObject> sink = Sinks.many().multicast().onBackpressureBuffer();
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
            JSONObject json = toResponse(chatId, chatRecordId, text, false, 0, 0);
            sink.tryEmitNext(json);
        } else {
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                String text = directlyReturnChunkList.get(0).text();
                sink.tryEmitNext(toResponse(chatId, chatRecordId, text, false, 0, 0));
            } else if (paragraphList.isEmpty()) {
                String status = noReferencesSetting.getStatus();
                if ("designated_answer".equals(status)) {
                    String value = noReferencesSetting.getValue();
                    String text = value.replace("{question}", problemText);
                    sink.tryEmitNext(toResponse(chatId, chatRecordId, text, false, 0, 0));
                }
            } else {
                int messageTokens = manage.context.getInteger("messageTokens");
                int answerTokens = manage.context.getInteger("answerTokens");
                String clientId = manage.context.getString("client_id");
                String clientType = manage.context.getString("client_type");
                if (stream) {
                    Assistant assistant = AiServices.create(Assistant.class,chatModel.getStreamingChatModel());
                    TokenStream tokenStream = assistant.chatStream(messageList);
                    tokenStream.onPartialResponse(text -> sink.tryEmitNext(toResponse(chatId, chatRecordId, text, false, 0, 0)))
                            .onCompleteResponse(response->{
                                String  answerText=response.aiMessage().text();
                                TokenUsage tokenUsage=response.tokenUsage();
                                int thisMessageTokens = tokenUsage.inputTokenCount();
                                int thisAnswerTokens = tokenUsage.outputTokenCount();
                                manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                                manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                                addAccessNum(clientId, clientType);
                                postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);
                                sink.tryEmitNext(toResponse(chatId, chatRecordId, "", true, 0, 0));
                                sink.tryEmitComplete();
                            })
                            .onError(error->{
                                sink.tryEmitNext(toResponse(chatId, chatRecordId, "", true, 0, 0));
                                sink.tryEmitComplete();
                            })
                            .start();
                } else {
                    ChatResponse response = chatModel.generate(messageList);
                    String  answerText=response.aiMessage().text();
                    TokenUsage tokenUsage=response.tokenUsage();
                    sink.tryEmitNext(toResponse(chatId, chatRecordId, answerText, true, 0, 0));
                    sink.tryEmitComplete();
                    int thisMessageTokens = tokenUsage.inputTokenCount();
                    int thisAnswerTokens = tokenUsage.outputTokenCount();
                    manage.context.put("messageTokens", messageTokens + thisMessageTokens);
                    manage.context.put("answerTokens", answerTokens + thisAnswerTokens);
                    addAccessNum(clientId, clientType);
                    postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage, clientId);
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
        details.put("model_id", super.context.get("model_id"));
        details.put("message_list", resetMessageList(super.context.getJSONArray("message_list"), super.context.getString("answer_text")));
        details.put("messageTokens", super.context.get("messageTokens"));
        details.put("answerTokens", super.context.get("answerTokens"));
        details.put("cost", 0);
        return details;
    }

    public JSONObject toResponse(String chatId, String chatRecordId, String content, Boolean isEnd, int completionTokens,
                                 int promptTokens) {
        JSONObject data = new JSONObject();
        data.put("chat_id", chatId);
        data.put("chat_record_id", chatRecordId);
        data.put("operate", true);
        data.put("content", content);
        data.put("node_id", "ai-chat-node");
        data.put("node_type", "ai-chat-node");
        data.put("node_is_end", true);
        data.put("view_type", "many_view");
        data.put("is_end", isEnd);
        JSONObject usage = new JSONObject();
        usage.put("completion_tokens", completionTokens);
        usage.put("prompt_tokens", promptTokens);
        usage.put("total_tokens", (promptTokens + completionTokens));
        data.put("usage", usage);
        return data;
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
