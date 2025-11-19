package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.assistant.CompressingQueryAssistant;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ResetProblemStep extends IResetProblemStep {

    private final ModelFactory modelFactory;
    private final ChatMemoryStore chatMemoryStore;

    @Override
    protected String execute(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        ApplicationEntity application = (ApplicationEntity) manage.context.get("application");
        String modelId = application.getModelId();
        JSONObject modelParams = application.getModelParamsSetting();
        ChatModel chatModel = modelFactory.buildChatModel(modelId,modelParams);
        String question = (String) context.get("problemText");
        String chatId = (String) context.get("chatId");
       // String systemText = application.getModelSetting().getSystem();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(chatId)
                .maxMessages(application.getDialogueNumber())
                .chatMemoryStore(chatMemoryStore)
                .build();
        CompressingQueryAssistant queryAssistant = AiServices.builder(CompressingQueryAssistant.class)
                .chatModel(chatModel)
                .build();
        Result<String> result= queryAssistant.transform(MessageTools.format(chatMemory.messages()),question);
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
        details.put("messageTokens", context.get("messageTokens"));
        details.put("answerTokens", context.get("answerTokens"));
        return details;
    }
}
