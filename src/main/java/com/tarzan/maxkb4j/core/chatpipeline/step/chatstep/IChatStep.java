package com.tarzan.maxkb4j.core.chatpipeline.step.chatstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;

import java.util.List;

public abstract class IChatStep extends IChatPipelineStep {
    public final String viewType = "many_view";

    @Override
    protected void _run(PipelineManage manage) {
        String answer = execute(manage);
        manage.context.put("answer", answer);
    }

    protected abstract String execute(PipelineManage manage);


    public ChatMessageVO toChatMessageVO(String chatId, String chatRecordId, String content, String reasoningContent, boolean nodeIsEnd) {
        return new ChatMessageVO(
                chatId,
                chatRecordId,
                "ai-chat-node",
                content,
                reasoningContent,
                List.of(),
                null,
                "ai-chat-node",
                "many_view",
                null,
                nodeIsEnd,
                nodeIsEnd);
    }
}
