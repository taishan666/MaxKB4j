package com.tarzan.maxkb4j.module.chatpipeline.chatstep.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.PostResponseHandler;
import com.tarzan.maxkb4j.module.chatpipeline.chatstep.IChatStep;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseChatStep extends IChatStep {


    @Override
    protected JSONObject execute(PipelineManage manage) throws Exception {
        JSONObject context = manage.getContext();
        ApplicationEntity application = context.getJSONObject("application").toJavaObject(ApplicationEntity.class);
        List<ChatMessage> messages =(List<ChatMessage>) context.get("messageList");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) context.get("paragraphList");
        UUID modelId = application.getModelId();
        ModelService modelService= SpringUtil.getBean(ModelService.class);
        ChatLanguageModel chatModel=modelService.getChatModelById(modelId);
     /*   ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .build();
        ChatResponse chatResponse = chatModel.chat(chatRequest);
        System.out.println("chatResponse: "+chatResponse);*/
        PostResponseHandler postResponseHandler= (PostResponseHandler) context.get("postResponseHandler");
        JSONObject modelSetting = application.getModelSetting();
        String problemText = modelSetting.getString("prompt");
        JSONObject datasetSetting = application.getDatasetSetting();
        JSONObject noReferencesSetting=datasetSetting.getJSONObject("no_references_setting");
        UUID chatId=UUID.fromString(context.getString("chatId"));
        return executeBlock(chatId,messages,chatModel,paragraphList,noReferencesSetting,manage,problemText,postResponseHandler);
    }

    protected JSONObject executeBlock(UUID chatId,
            List<ChatMessage> messageList,
                                     ChatLanguageModel chatModel,
                                     List<ParagraphVO> paragraphList,
                                     JSONObject noReferencesSetting,
                                     PipelineManage manage,
                                     String problemText,   PostResponseHandler postResponseHandler) {
        Response<AiMessage> res=getBlockResult(messageList,chatModel,paragraphList,noReferencesSetting,problemText);
        UUID chatRecordId=UUID.randomUUID();
        int messageTokens=res.tokenUsage().inputTokenCount();
        int answerTokens=res.tokenUsage().outputTokenCount();
        manage.getContext().put("message_tokens", messageTokens);
        manage.getContext().put("answer_tokens", answerTokens);
        long startTime=manage.getContext().getLong("start_time");
        manage.getContext().put("run_time", System.currentTimeMillis()-startTime);
        postResponseHandler.handler(chatId, chatRecordId, paragraphList, problemText,
                res.content().text(), manage, null, null);
        return manage.getBaseToResponse().toBlockResponse(chatId,chatRecordId,res.content().text(),true,answerTokens,messageTokens,null);
    }


    private Response<AiMessage> getBlockResult(List<ChatMessage> messageList,
                                ChatLanguageModel chatModel,
                                List<ParagraphVO> paragraphList,
                                JSONObject noReferencesSetting,
                                String problemText){

        if(CollectionUtils.isEmpty(paragraphList)){
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList=new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
           if("directly_return".equals(paragraph.getHitHandlingMethod())){
               directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
           }
        }
        TokenUsage tokenUsage=new TokenUsage(0,0,0);
        String status=noReferencesSetting.getString("status");
        if (!CollectionUtils.isEmpty(directlyReturnChunkList)){
            return Response.from(directlyReturnChunkList.get(0),tokenUsage);
        }else if(paragraphList.isEmpty() &&"designated_answer".equals(status)){
            String value=noReferencesSetting.getString("value");
            return Response.from(AiMessage.from(value.replace("{question}", problemText)),tokenUsage);
        }
        if (chatModel==null){
            return Response.from(AiMessage.from("抱歉，没有配置 AI 模型，无法优化引用分段，请先去应用中设置 AI 模型。"),tokenUsage);
        }else {
            return chatModel.generate(messageList);
        }

    }

    public List<ChatMessage> resetMessageList(JSONArray messageList, String answerText){
        List<ChatMessage> messages=messageList.toJavaList(ChatMessage.class);
        messages.add(AiMessage.from(answerText));
        return messages;
    }



    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","chat_step");
        details.put("run_time",super.context.get("run_time"));
        details.put("model_id",super.context.get("model_id"));
        details.put("message_list",resetMessageList(super.context.getJSONArray("messageList"),super.context.getString("answer_text")));
        details.put("message_tokens",super.context.get("message_tokens"));
        details.put("answer_tokens",super.context.get("answer_tokens"));
        details.put("cost",0);
        return details;
    }
}
