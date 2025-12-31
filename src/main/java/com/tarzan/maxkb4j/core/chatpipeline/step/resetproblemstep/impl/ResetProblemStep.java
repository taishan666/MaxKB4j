package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.assistant.CompressingQueryAssistant;
import com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.common.util.MessageUtils;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResetProblemStep extends IResetProblemStep {

    private final ModelFactory modelFactory;

    @Override
    protected String execute( String modelId,JSONObject modelParams, String question, List<ChatMessage> chatMemory) {
        long startTime = System.currentTimeMillis();
        ChatModel chatModel = modelFactory.buildChatModel(modelId,modelParams);
        // String systemText = application.getModelSetting().getSystem();
        CompressingQueryAssistant queryAssistant = AiServices.builder(CompressingQueryAssistant.class)
                .chatModel(chatModel)
                .build();
        Result<String> result= queryAssistant.transform(MessageUtils.format(chatMemory),question);
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
