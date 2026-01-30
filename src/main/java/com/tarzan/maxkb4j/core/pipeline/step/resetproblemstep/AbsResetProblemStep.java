package com.tarzan.maxkb4j.core.pipeline.step.resetproblemstep;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.pipeline.AbsStep;
import com.tarzan.maxkb4j.core.pipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public abstract class AbsResetProblemStep extends AbsStep {

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
