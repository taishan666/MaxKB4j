package com.tarzan.maxkb4j.core.chatpipeline.step.chatstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;

public abstract class IChatStep extends IChatPipelineStep {
    public final String viewType = "many_view";

    @Override
    protected void _run(PipelineManage manage) {
        String answer = execute(manage);
        manage.context.put("answer", answer);
    }

    protected abstract String execute(PipelineManage manage);
}
