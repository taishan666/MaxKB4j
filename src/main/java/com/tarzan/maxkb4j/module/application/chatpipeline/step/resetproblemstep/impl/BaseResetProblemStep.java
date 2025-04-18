package com.tarzan.maxkb4j.module.application.chatpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.rag.MyCompressingQueryTransformer;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.LlmModelSetting;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.util.TokenUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BaseResetProblemStep extends IResetProblemStep {

    private final ModelService modelService;

    private final static String prompt = "()里面是用户问题,根据上下文回答揣测用户问题({question}) 要求: 输出一个补全问题,并且放在<data></data>标签中";

    @Override
    protected String execute(PipelineManage manage) {
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
    }


    protected String execute1(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        List<ApplicationChatRecordEntity> chatRecordList = (List<ApplicationChatRecordEntity>) context.get("chatRecordList");
        String problemOptimizationPrompt = application.getProblemOptimizationPrompt();
        int startIndex = 0;
        if (chatRecordList.size() > 3) {
            startIndex = chatRecordList.size() - 3;
        }
        chatRecordList=chatRecordList.subList(startIndex, chatRecordList.size());
        List<ChatMessage> historyMessages=new ArrayList<>();
        for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
            historyMessages.add(UserMessage.from(chatRecord.getProblemText()));
            historyMessages.add(AiMessage.from(chatRecord.getAnswerText()));
        }
        String modelId = application.getModelId();
        BaseChatModel chatModel = modelService.getModelById(modelId);
        String resetPrompt = StringUtils.isNotBlank(problemOptimizationPrompt) ? problemOptimizationPrompt : prompt;
        String problemText = context.getString("problem_text");
        historyMessages.add(UserMessage.from(resetPrompt.replace("{question}", problemText)));
        ChatResponse res = chatModel.generate(historyMessages);
        String content=res.aiMessage().text();
        String paddingProblem = getString(content);
        TokenUsage tokenUsage = res.tokenUsage();
        super.context.put("modelId", modelId);
        super.context.put("problem_text", context.getString("problem_text"));
        super.context.put("messageTokens", tokenUsage.inputTokenCount());
        super.context.put("answerTokens", tokenUsage.outputTokenCount());
        super.context.put("padding_problem_text", paddingProblem);
        System.out.println("BaseResetProblemStep 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
        return paddingProblem;
    }

    private String getString(String content) {
        String paddingProblem = "";
        if (content.contains("<data>") && content.contains("</data>")) {
            int start = content.indexOf("<data>") + 6; // 加6是因为"<data>".length()等于6
            int end = content.indexOf("</data>");
            String paddingProblemData = content.substring(start, end);
            if (!paddingProblemData.trim().isEmpty()) {
                paddingProblem = paddingProblemData;
            }
        } else if (!content.isEmpty()) {
            paddingProblem = content;
        }
        return paddingProblem;
    }

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
