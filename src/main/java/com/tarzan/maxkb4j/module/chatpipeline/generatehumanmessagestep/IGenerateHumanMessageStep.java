package com.tarzan.maxkb4j.module.chatpipeline.generatehumanmessagestep;

import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class IGenerateHumanMessageStep extends IBaseChatPipelineStep {
    @Override
    public void _run(PipelineManage manage) {
        try {
            List<ChatMessage> messageList= execute(manage);
            manage.getContext().put("messageList",messageList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected abstract List<ChatMessage> execute(PipelineManage manage) throws Exception;
}
