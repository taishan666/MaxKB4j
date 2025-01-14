package com.tarzan.maxkb4j.module.chatpipeline.step.chatstep;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public abstract class IChatStep extends IBaseChatPipelineStep {

    @Override
    protected Object _run(PipelineManage manage) {
        try {
           // Flux<JSONObject>  chat_result=
          //  manage.context.put("chat_result",chat_result);
            return execute(manage);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    protected abstract Flux<JSONObject> execute(PipelineManage manage) throws Exception;
}
