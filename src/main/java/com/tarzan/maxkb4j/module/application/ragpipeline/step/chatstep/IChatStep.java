package com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep;

import com.tarzan.maxkb4j.module.application.ragpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;

public abstract class IChatStep extends IBaseChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
         manage.response=execute(manage);
    }

    protected abstract ChatMessageVO execute(PipelineManage manage);
}
