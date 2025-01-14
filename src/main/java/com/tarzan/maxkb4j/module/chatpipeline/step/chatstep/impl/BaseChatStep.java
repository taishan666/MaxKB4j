package com.tarzan.maxkb4j.module.chatpipeline.step.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BaseChatStep extends IChatStep {

    @Autowired
    private ModelService modelService;

    @Override
    protected Flux<JSONObject> execute(PipelineManage manage) throws Exception {
        JSONObject context = manage.context;
        ApplicationEntity application = context.getJSONObject("application").toJavaObject(ApplicationEntity.class);
        List<ChatMessage> messages = (List<ChatMessage>) context.get("messageList");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraphList");
        UUID modelId = application.getModelId();
        PostResponseHandler postResponseHandler = (PostResponseHandler) context.get("postResponseHandler");
        String problemText = context.getString("problem_text");
        JSONObject datasetSetting = application.getDatasetSetting();
        JSONObject noReferencesSetting = datasetSetting.getJSONObject("no_references_setting");
        UUID chatId = UUID.fromString(context.getString("chatId"));
        return executeStream(chatId, messages, modelId, paragraphList, noReferencesSetting, manage, problemText, postResponseHandler);
    }

    protected Flux<JSONObject> executeStream(UUID chatId,
                                             List<ChatMessage> messageList,
                                             UUID modelId,
                                             List<ParagraphVO> paragraphList,
                                             JSONObject noReferencesSetting,
                                             PipelineManage manage,
                                             String problemText, PostResponseHandler postResponseHandler) {
     //   ModelService modelService = SpringUtil.getBean(ModelService.class);
        StreamingChatLanguageModel chatModel = modelService.getStreamingChatModelById(modelId);
        UUID chatRecordId = UUID.randomUUID();
        JSONObject selfContext=super.context;
        // 初始化一个可变的Publisher，如Sinks.many()来代替Flux.just()
        Sinks.Many<JSONObject> sink = Sinks.many().multicast().onBackpressureBuffer();
        StreamingResponseHandler<AiMessage> responseHandler = new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                JSONObject json=manage.baseToResponse.toBlockResponse(chatId, chatRecordId, token, false, 0, 0, null);
                sink.tryEmitNext(json);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                TokenUsage tokenUsage=response.tokenUsage();
                String answerText=response.content().text();
                int messageTokens = tokenUsage.inputTokenCount();
                int answerTokens = tokenUsage.outputTokenCount();
                selfContext.put("message_list", messageList);
                selfContext.put("answer_text", answerText);
                manage.context.put("message_tokens", messageTokens);
                selfContext.put("message_tokens", messageTokens);
                manage.context.put("answer_tokens", answerTokens);
                selfContext.put("answer_tokens", answerTokens);
                long startTime = manage.context.getLong("start_time");
                manage.context.put("run_time", (System.currentTimeMillis() - startTime) / 1000F);
                postResponseHandler.handler(ChatCache.get(chatId),chatId, chatRecordId, paragraphList, problemText, answerText, manage, null, null);
                JSONObject json=manage.baseToResponse.toBlockResponse(chatId, chatRecordId, "", true, tokenUsage.outputTokenCount(), tokenUsage.inputTokenCount(), null);
                sink.tryEmitNext(json);
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                JSONObject json=manage.baseToResponse.toBlockResponse(chatId, chatRecordId, error.getMessage(), true, 0, 0, null);
                sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST);
                sink.emitError(error,Sinks.EmitFailureHandler.FAIL_FAST);
            }
        };
        getStreamResult(messageList, chatModel, paragraphList, noReferencesSetting, problemText, responseHandler);
        return sink.asFlux();
    }


    private void getStreamResult(List<ChatMessage> messageList,
                                 StreamingChatLanguageModel chatModel,
                                 List<ParagraphVO> paragraphList,
                                 JSONObject noReferencesSetting,
                                 String problemText, StreamingResponseHandler<AiMessage> responseHandler) {

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
        }
        if (chatModel == null) {
            String text = "抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。";
            Response<AiMessage> res = Response.from(AiMessage.from(text), tokenUsage);
            responseHandler.onNext(text);
            responseHandler.onComplete(res);
        } else {
            chatModel.generate(messageList, responseHandler);
        }
    }

    protected Flux<JSONObject> executeBlock(UUID chatId,
                                      List<ChatMessage> messageList,
                                      UUID modelId,
                                      List<ParagraphVO> paragraphList,
                                      JSONObject noReferencesSetting,
                                      PipelineManage manage,
                                      String problemText, PostResponseHandler postResponseHandler) {
      //  ModelService modelService = SpringUtil.getBean(ModelService.class);
        ChatLanguageModel chatModel = modelService.getChatModelById(modelId);
        Response<AiMessage> res = getBlockResult(messageList, chatModel, paragraphList, noReferencesSetting, problemText);
        UUID chatRecordId = UUID.randomUUID();
        int messageTokens = res.tokenUsage().inputTokenCount();
        int answerTokens = res.tokenUsage().outputTokenCount();
        super.context.put("message_list", messageList);
        super.context.put("answer_text", res.content().text());
        manage.context.put("message_tokens", messageTokens);
        super.context.put("message_tokens", messageTokens);
        manage.context.put("answer_tokens", answerTokens);
        super.context.put("answer_tokens", answerTokens);
        long startTime = manage.context.getLong("start_time");
        manage.context.put("run_time", (System.currentTimeMillis() - startTime) / 1000F);
        postResponseHandler.handler(ChatCache.get(chatId),chatId, chatRecordId, paragraphList, problemText,
                res.content().text(), manage, null, null);
        JSONObject json=manage.baseToResponse.toBlockResponse(chatId, chatRecordId, res.content().text(), true, answerTokens, messageTokens, null);
        return Flux.just(json);
    }


    private Response<AiMessage> getBlockResult(List<ChatMessage> messageList,
                                               ChatLanguageModel chatModel,
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

    public List<ChatMessage> resetMessageList(JSONArray messageList, String answerText) {
        if (CollectionUtils.isEmpty(messageList)) {
            return new ArrayList<>();
        }
        List<ChatMessage> messages = new ArrayList<>();
        for (Object o : messageList) {
            messages.add((ChatMessage) o);
        }
        messages.add(AiMessage.from(answerText));
        return messages;
    }


    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject();
        details.put("step_type", "chat_step");
        details.put("run_time", super.context.get("run_time"));
        details.put("model_id", super.context.get("model_id"));
        details.put("message_list", resetMessageList(super.context.getJSONArray("message_list"), super.context.getString("answer_text")));
        details.put("message_tokens", super.context.get("message_tokens"));
        details.put("answer_tokens", super.context.get("answer_tokens"));
        details.put("cost", 0);
        return details;
    }
}
