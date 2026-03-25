package com.maxkb4j.application.executor;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;
import com.maxkb4j.common.enums.ChatUserType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import reactor.core.publisher.Sinks;

import java.util.Map;

public class AgentExecutor extends AbsToolExecutor{

    private final String appId;
    private final IApplicationChatService chatService;

    public AgentExecutor(String appId, IApplicationChatService chatService) {
        this.appId = appId;
        this.chatService = chatService;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> args = argumentsAsMap(toolExecutionRequest.arguments());
        String message = (String) args.getOrDefault("message","");
        ChatParams params = ChatParams.builder()
                .message(message)
                .reChat(false)
                .stream(false)
                .appId(appId)
                .chatId(String.valueOf(memoryId))
                .chatUserId(IdWorker.get32UUID())
                .chatUserType(ChatUserType.ANONYMOUS_USER.name())
                .debug(false)
                .build();
        ChatResponse chatResponse = chatService.chatMessage(params, Sinks.many().unicast().onBackpressureBuffer());
        return chatResponse.getAnswer();
    }

}

