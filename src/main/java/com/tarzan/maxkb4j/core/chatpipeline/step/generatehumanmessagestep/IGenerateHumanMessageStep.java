package com.tarzan.maxkb4j.core.chatpipeline.step.generatehumanmessagestep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;

public abstract class IGenerateHumanMessageStep extends IChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        String prompt = execute(manage);
        manage.context.put("user_prompt", prompt);
    }

    protected abstract String execute(PipelineManage manage);
}
