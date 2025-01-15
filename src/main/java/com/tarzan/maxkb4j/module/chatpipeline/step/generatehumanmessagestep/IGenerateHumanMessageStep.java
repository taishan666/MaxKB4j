package com.tarzan.maxkb4j.module.chatpipeline.step.generatehumanmessagestep;

import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public abstract class IGenerateHumanMessageStep extends IBaseChatPipelineStep {
    @Override
    protected void _run(PipelineManage manage) {
        List<ChatMessage> messageList = execute(manage);
        manage.context.put("message_list", messageList);
    }

    protected abstract List<ChatMessage> execute(PipelineManage manage);
}
