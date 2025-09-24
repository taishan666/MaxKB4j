package com.tarzan.maxkb4j.module.application.ragpipeline.generatehumanmessagestep;

import com.tarzan.maxkb4j.module.application.ragpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;

public abstract class IGenerateHumanMessageStep extends IBaseChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        String prompt = execute(manage);
        manage.context.put("user_prompt", prompt);
        super.context.put("user_prompt", prompt);
    }

    protected abstract String execute(PipelineManage manage);
}
