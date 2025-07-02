package com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep;

import com.tarzan.maxkb4j.module.application.ragpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;

public abstract class IChatStep extends IBaseChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
         manage.answer=execute(manage);
    }

    protected abstract String execute(PipelineManage manage);
}
