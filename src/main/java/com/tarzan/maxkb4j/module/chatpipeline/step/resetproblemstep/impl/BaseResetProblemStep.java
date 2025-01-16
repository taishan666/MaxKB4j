package com.tarzan.maxkb4j.module.chatpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BaseResetProblemStep extends IResetProblemStep {

    @Autowired
    private ModelService modelService;

    private final static String prompt = "()里面是用户问题,根据上下文回答揣测用户问题({question}) 要求: 输出一个补全问题,并且放在<data></data>标签中";

    @Override
    protected String execute(PipelineManage manage) {
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
        UUID modelId = application.getModelId();
        ChatLanguageModel chatModel = modelService.getChatModelById(modelId);
        String resetPrompt = StringUtils.isNotBlank(problemOptimizationPrompt) ? problemOptimizationPrompt : prompt;
        String problemText = context.getString("problem_text");
        historyMessages.add(UserMessage.from(resetPrompt.replace("{question}", problemText)));
        Response<AiMessage> res = chatModel.generate(historyMessages);
        String content=res.content().text();
        String paddingProblem = getString(content);
        TokenUsage tokenUsage = res.tokenUsage();
        super.context.put("model_id", modelId);
        super.context.put("problem_text", context.getString("problem_text"));
        super.context.put("message_tokens", tokenUsage.inputTokenCount());
        super.context.put("answer_tokens", tokenUsage.outputTokenCount());
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
        details.put("model_id",super.context.get("model_id"));
        details.put("run_time",super.context.get("run_time"));
        details.put("problem_text",super.context.get("problem_text"));
        details.put("padding_problem_text",super.context.get("padding_problem_text"));
        details.put("message_tokens", super.context.get("message_tokens"));
        details.put("answer_tokens", super.context.get("answer_tokens"));
        details.put("cost",0);
        return details;
    }
}
