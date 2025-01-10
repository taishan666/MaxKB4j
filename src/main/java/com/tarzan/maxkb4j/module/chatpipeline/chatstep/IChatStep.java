package com.tarzan.maxkb4j.module.chatpipeline.chatstep;

import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public abstract class IChatStep extends IBaseChatPipelineStep {

    @Override
    public void _run(PipelineManage manage) {
        try {
            Map<String, Object> chat_result= execute(manage);
            manage.getContext().put("chat_result",chat_result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected abstract Map<String, Object> execute(PipelineManage manage) throws Exception;
}
