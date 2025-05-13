package com.tarzan.maxkb4j.module.application.ragpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.rag.MyChatMemory;
import com.tarzan.maxkb4j.module.rag.MyCompressingQueryTransformer;
import com.tarzan.maxkb4j.util.TokenUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@AllArgsConstructor
public class BaseResetProblemStep extends IResetProblemStep {

    private final ModelService modelService;
    private final ChatMemoryStore chatMemoryStore;

    @Override
    protected String execute(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        String modelId = application.getModelId();
        BaseChatModel chatModel = modelService.getModelById(modelId);
        QueryTransformer queryTransformer=new MyCompressingQueryTransformer(chatModel.getChatModel());
        String question = context.getString("problem_text");
        String chatId = context.getString("chatId");
        ChatMemory chatMemory = MyChatMemory.builder()
                .id(chatId)
                .maxMessages(application.getDialogueNumber())
                .chatMemoryStore(chatMemoryStore)
                .build();
        Metadata metadata=new Metadata(UserMessage.from(question), chatMemory.id(), chatMemory.messages());
        Query query=new Query(question,metadata);
        Collection<Query> list= queryTransformer.transform(query);
        StringBuilder answerSb=new StringBuilder();
        for (Query queryResult : list) {
            answerSb.append(queryResult.text());
        }
        String paddingProblem=answerSb.toString();
        super.context.put("modelId", modelId);
        super.context.put("problem_text", question);
        super.context.put("messageTokens", TokenUtil.countTokens(chatMemory.messages()));
        super.context.put("answerTokens", TokenUtil.countTokens(paddingProblem));
        super.context.put("padding_problem_text", paddingProblem);
        log.info("BaseResetProblemStep 耗时 {} ms", System.currentTimeMillis() - startTime);
        return paddingProblem;
    }


/*    protected String execute1(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        List<ApplicationChatRecordEntity> chatRecordList = (List<ApplicationChatRecordEntity>) context.get("chatRecordList");
        //TODO 按照设定聊天记录更好，还是取近3条的更好，需要验证,数量越小接口返回越快
        int messageNum=chatRecordList.size();
        chatRecordList=messageNum>3?chatRecordList.subList(messageNum-3,messageNum):chatRecordList;
        List<ChatMessage> historyMessages=new ArrayList<>();
        LlmModelSetting modelSetting = application.getModelSetting();
        String system = modelSetting.getSystem();
        // String prompt = modelSetting.getPrompt();
        historyMessages.add(SystemMessage.from(system));
        for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
            historyMessages.add(UserMessage.from(chatRecord.getProblemText()));
            //  historyMessages.add(AiMessage.from(chatRecord.getAnswerText()));
        }
        String modelId = application.getModelId();
        BaseChatModel chatModel = modelService.getModelById(modelId);
        QueryTransformer queryTransformer=new MyCompressingQueryTransformer(chatModel.getChatModel());
        String question = context.getString("problem_text");
        String chatId = context.getString("chatId");
        Metadata metadata=new Metadata(UserMessage.from(question), chatId, historyMessages);
        Query query=new Query(question,metadata);
        Collection<Query> list= queryTransformer.transform(query);
        StringBuilder answerSb=new StringBuilder();
        for (Query queryResult : list) {
            answerSb.append(queryResult.text());
        }
        String paddingProblem=answerSb.toString();
        super.context.put("modelId", modelId);
        super.context.put("problem_text", question);
        super.context.put("messageTokens", TokenUtil.countTokens(historyMessages));
        super.context.put("answerTokens", TokenUtil.countTokens(paddingProblem));
        super.context.put("padding_problem_text", paddingProblem);
        log.info("BaseResetProblemStep 耗时 {} ms", System.currentTimeMillis() - startTime);
        return paddingProblem;
    }*/


    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","problem_padding");
        details.put("modelId",super.context.get("modelId"));
        details.put("runTime",super.context.get("runTime"));
        details.put("problem_text",super.context.get("problem_text"));
        details.put("padding_problem_text",super.context.get("padding_problem_text"));
        details.put("messageTokens", super.context.get("messageTokens"));
        details.put("answerTokens", super.context.get("answerTokens"));
        details.put("cost",0);
        return details;
    }
}
