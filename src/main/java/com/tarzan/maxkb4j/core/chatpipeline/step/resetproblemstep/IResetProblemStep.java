package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public abstract class IResetProblemStep extends IChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        ApplicationEntity application = manage.application;
        String modelId = application.getModelId();
        JSONObject modelParams = application.getModelParamsSetting();
        String question = manage.chatParams.getMessage();
        List<ChatMessage> chatMemory= manage.getHistoryMessages(application.getDialogueNumber());
        String paddingProblemText = execute(modelId,modelParams, question,chatMemory);
        manage.context.put("paddingProblemText", paddingProblemText);
    }


    protected abstract String execute(String modelId,JSONObject modelParams, String question, List<ChatMessage> chatMemory);
}
