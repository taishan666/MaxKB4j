package com.tarzan.maxkb4j.module.chatpipeline.step.generatehumanmessagestep;

import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class IGenerateHumanMessageStep extends IBaseChatPipelineStep {
    @Override
    protected Object _run(PipelineManage manage) {
        try {
            List<ChatMessage> messageList= execute(manage);
            manage.context.put("messageList",messageList);
            return messageList;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    protected abstract List<ChatMessage> execute(PipelineManage manage) throws Exception;
}
