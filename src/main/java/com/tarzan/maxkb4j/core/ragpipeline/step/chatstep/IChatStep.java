package com.tarzan.maxkb4j.core.ragpipeline.step.chatstep;

import com.tarzan.maxkb4j.core.ragpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.ragpipeline.PipelineManage;

public abstract class IChatStep extends IChatPipelineStep {
    public final String viewType="many_view";

    @Override
    protected void _run(PipelineManage manage) {
         manage.answer=execute(manage);
    }

    protected abstract String execute(PipelineManage manage);
}
