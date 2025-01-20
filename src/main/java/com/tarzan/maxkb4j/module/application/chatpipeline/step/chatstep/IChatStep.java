package com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import reactor.core.publisher.Flux;

public abstract class IChatStep extends IBaseChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        manage.response = execute(manage);
    }

    protected abstract Flux<JSONObject> execute(PipelineManage manage);
}
