package com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.module.application.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

@Component
public class BaseChatStep extends IChatStep {

    @Autowired
    private ModelService modelService;
    @Autowired
    private ApplicationPublicAccessClientService publicAccessClientService;

    @Override
    protected Flux<JSONObject> execute(PipelineManage manage) {
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        List<ChatMessage> messages = (List<ChatMessage>) context.get("message_list");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraph_list");
        String modelId = application.getModelId();
        super.context.put("model_id", modelId);
        PostResponseHandler postResponseHandler = (PostResponseHandler) context.get("postResponseHandler");
        String problemText = context.getString("problem_text");
        JSONObject datasetSetting = application.getDatasetSetting();
        JSONObject noReferencesSetting = datasetSetting.getJSONObject("no_references_setting");
        String chatId = context.getString("chatId");
        return executeStream(chatId, messages, modelId, paragraphList, noReferencesSetting, manage, problemText, postResponseHandler);
    }

    protected Flux<JSONObject> executeStream(String chatId,
                                             List<ChatMessage> messageList,
                                             String modelId,
                                             List<ParagraphVO> paragraphList,
                                             JSONObject noReferencesSetting,
                                             PipelineManage manage,
                                             String problemText, PostResponseHandler postResponseHandler) {
        long startTime = System.currentTimeMillis();
        BaseChatModel chatModel = modelService.getModelById(modelId);
        JSONObject selfContext = super.context;
        // 初始化一个可变的Publisher，如Sinks.many()来代替Flux.just()
        Sinks.Many<JSONObject> sink = Sinks.many().multicast().onBackpressureBuffer();
        StreamingResponseHandler<AiMessage> responseHandler = new StreamingResponseHandler<>() {
            final String chatRecordId = IdWorker.get32UUID();

            @Override
            public void onNext(String token) {
                JSONObject json = toResponse(chatId, chatRecordId, token, false, 0, 0);
                sink.tryEmitNext(json);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                TokenUsage tokenUsage = response.tokenUsage();
                String answerText = response.content().text();
                selfContext.put("message_list", messageList);
                selfContext.put("answer_text", answerText);
                int thisMessageTokens = tokenUsage.inputTokenCount();
                int thisAnswerTokens = tokenUsage.outputTokenCount();
                int messageTokens = manage.context.getInteger("message_tokens");
                int answerTokens = manage.context.getInteger("answer_tokens");
                selfContext.put("message_tokens", thisMessageTokens);
                selfContext.put("answer_tokens", thisAnswerTokens);
                manage.context.put("message_tokens", messageTokens + thisMessageTokens);
                manage.context.put("answer_tokens", answerTokens + thisAnswerTokens);
                String clientId=manage.context.getString("client_id");
                String clientType=manage.context.getString("client_type");
                addAccessNum(clientId,clientType);
                postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText, answerText, manage,  clientId);
                JSONObject json = toResponse(chatId, chatRecordId, "", true, tokenUsage.outputTokenCount(), tokenUsage.inputTokenCount());
                sink.tryEmitNext(json);
                sink.tryEmitComplete();
                System.out.println("BaseChatStep 耗时 " + (System.currentTimeMillis() - startTime) + " ms");
            }

            @Override
            public void onError(Throwable error) {
                JSONObject json = toResponse(chatId, chatRecordId, "网络异常！请重试。。。", true, 0, 0);
                sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST);
                sink.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST);
            }
        };
        getStreamResult(messageList, chatModel, paragraphList, noReferencesSetting, problemText, responseHandler);
        return sink.asFlux();
    }


    private void getStreamResult(List<ChatMessage> messageList,
                                 BaseChatModel chatModel,
                                 List<ParagraphVO> paragraphList,
                                 JSONObject noReferencesSetting,
                                 String problemText, StreamingResponseHandler<AiMessage> responseHandler) {

        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
            if ("directly_return".equals(paragraph.getHitHandlingMethod())&&paragraph.getSimilarity() >= paragraph.getDirectlyReturnSimilarity()) {
                directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
            }
        }
        TokenUsage tokenUsage = new TokenUsage(0, 0, 0);
        if (chatModel == null) {
            String text = "抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。";
            Response<AiMessage> res = Response.from(AiMessage.from(text), tokenUsage);
            responseHandler.onNext(text);
            responseHandler.onComplete(res);
        } else {
            String status = noReferencesSetting.getString("status");
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                String text = directlyReturnChunkList.get(0).text();
                Response<AiMessage> res = Response.from(AiMessage.from(text), tokenUsage);
                responseHandler.onNext(text);
                responseHandler.onComplete(res);
            } else if (paragraphList.isEmpty() && "designated_answer".equals(status)) {
                String value = noReferencesSetting.getString("value");
                String text = value.replace("{question}", problemText);
                Response<AiMessage> res = Response.from(AiMessage.from(text), tokenUsage);
                responseHandler.onNext(text);
                responseHandler.onComplete(res);
            }else {
                chatModel.stream(messageList, responseHandler);
            }
        }
    }

    protected Flux<JSONObject> executeBlock(String chatId,
                                            List<ChatMessage> messageList,
                                            String modelId,
                                            List<ParagraphVO> paragraphList,
                                            JSONObject noReferencesSetting,
                                            PipelineManage manage,
                                            String problemText, PostResponseHandler postResponseHandler) {
        BaseChatModel chatModel = modelService.getModelById(modelId);
        Response<AiMessage> res = getBlockResult(messageList, chatModel, paragraphList, noReferencesSetting, problemText);
        String chatRecordId = IdWorker.get32UUID();
        super.context.put("message_list", messageList);
        super.context.put("answer_text", res.content().text());
        int thisMessageTokens = res.tokenUsage().inputTokenCount();
        int thisAnswerTokens = res.tokenUsage().outputTokenCount();
        int messageTokens = manage.context.getInteger("message_tokens");
        int answerTokens = manage.context.getInteger("answer_tokens");
        super.context.put("message_tokens", thisMessageTokens);
        super.context.put("answer_tokens", thisAnswerTokens);
        manage.context.put("message_tokens", messageTokens + thisMessageTokens);
        manage.context.put("answer_tokens", answerTokens + thisAnswerTokens);
        long startTime = manage.context.getLong("start_time");
        manage.context.put("run_time", (System.currentTimeMillis() - startTime) / 1000F);
        String clientId=manage.context.getString("client_id");
        postResponseHandler.handler(ChatCache.get(chatId), chatId, chatRecordId, problemText,
                res.content().text(), manage,  clientId);
        JSONObject json = toResponse(chatId, chatRecordId, res.content().text(), true, answerTokens, messageTokens);
        return Flux.just(json);
    }


    private Response<AiMessage> getBlockResult(List<ChatMessage> messageList,
                                               BaseChatModel chatModel,
                                               List<ParagraphVO> paragraphList,
                                               JSONObject noReferencesSetting,
                                               String problemText) {

        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
            if ("directly_return".equals(paragraph.getHitHandlingMethod())) {
                directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
            }
        }
        TokenUsage tokenUsage = new TokenUsage(0, 0, 0);
        String status = noReferencesSetting.getString("status");
        if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
            return Response.from(directlyReturnChunkList.get(0), tokenUsage);
        } else if (paragraphList.isEmpty() && "designated_answer".equals(status)) {
            String value = noReferencesSetting.getString("value");
            return Response.from(AiMessage.from(value.replace("{question}", problemText)), tokenUsage);
        }
        if (chatModel == null) {
            return Response.from(AiMessage.from("抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。"), tokenUsage);
        } else {
            return chatModel.generate(messageList);
        }

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
        details.put("run_time", (System.currentTimeMillis() - startTime) / 1000F);
        details.put("model_id", super.context.get("model_id"));
        details.put("message_list", resetMessageList(super.context.getJSONArray("message_list"), super.context.getString("answer_text")));
        details.put("message_tokens", super.context.get("message_tokens"));
        details.put("answer_tokens", super.context.get("answer_tokens"));
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

    private void addAccessNum(String clientId, String clientType){
        if("APPLICATION_ACCESS_TOKEN".equals(clientType)){
            ApplicationPublicAccessClientEntity publicAccessClient=publicAccessClientService.getById(clientId);
            if(publicAccessClient!=null){
                publicAccessClient.setAccessNum(publicAccessClient.getAccessNum()+1);
                publicAccessClient.setIntraDayAccessNum(publicAccessClient.getIntraDayAccessNum()+1);
                publicAccessClientService.updateById(publicAccessClient);
            }
        }
    }

}
