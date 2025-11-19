package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.assistant.CompressingQueryAssistant;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ResetProblemStep extends IResetProblemStep {

    private final ModelFactory modelFactory;

    @Override
    protected String execute(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        ApplicationEntity application = (ApplicationEntity) manage.context.get("application");
        String modelId = application.getModelId();
        JSONObject modelParams = application.getModelParamsSetting();
        ChatModel chatModel = modelFactory.buildChatModel(modelId,modelParams);
        String question = (String) manage.context.get("problemText");
       // String systemText = application.getModelSetting().getSystem();
        List<ChatMessage> chatMemory= manage.getHistoryMessages(application.getDialogueNumber());
        CompressingQueryAssistant queryAssistant = AiServices.builder(CompressingQueryAssistant.class)
                .chatModel(chatModel)
                .build();
        Result<String> result= queryAssistant.transform(MessageTools.format(chatMemory),question);
        String paddingProblem=result.content();
        super.context.put("modelId", modelId);
        super.context.put("problemText", question);
        TokenUsage tokenUsage=result.tokenUsage();
        super.context.put("messageTokens", tokenUsage.inputTokenCount());
        super.context.put("answerTokens", tokenUsage.inputTokenCount());
        super.context.put("paddingProblemText", paddingProblem);
        log.info("BaseResetProblemStep 耗时 {} ms", System.currentTimeMillis() - startTime);
        return paddingProblem;
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","problem_padding");
        details.put("problemText",context.get("problemText"));
        details.put("paddingProblemText",context.get("paddingProblemText"));
        details.put("runTime", context.get("runTime"));
        details.put("messageTokens", context.getOrDefault("messageTokens",0));
        details.put("answerTokens", context.getOrDefault("answerTokens",0));
        return details;
    }
}
