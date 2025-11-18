package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;

public abstract class IResetProblemStep extends IChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        String paddingProblemText = execute(manage);
        manage.context.put("paddingProblemText", paddingProblemText);
    }

    protected abstract String execute(PipelineManage manage);
}
