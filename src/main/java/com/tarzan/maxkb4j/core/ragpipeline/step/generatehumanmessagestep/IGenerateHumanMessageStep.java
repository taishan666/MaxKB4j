package com.tarzan.maxkb4j.core.ragpipeline.step.generatehumanmessagestep;

import com.tarzan.maxkb4j.core.ragpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.ragpipeline.PipelineManage;

public abstract class IGenerateHumanMessageStep extends IChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        String prompt = execute(manage);
        manage.context.put("user_prompt", prompt);
        super.context.put("user_prompt", prompt);
    }

    protected abstract String execute(PipelineManage manage);
}
