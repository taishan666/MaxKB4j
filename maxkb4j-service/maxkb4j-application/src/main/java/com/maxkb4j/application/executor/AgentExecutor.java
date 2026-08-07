package com.maxkb4j.application.executor;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.tool.executor.AbsToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import reactor.core.publisher.Sinks;

import java.util.Map;

public class AgentExecutor extends AbsToolExecutor {

    private final String chatUserId;
    private final String appId;
    private final IApplicationChatService chatService;

    public AgentExecutor(String appId, IApplicationChatService chatService) {
        this.chatUserId = IdWorker.get32UUID();
        this.appId = appId;
        this.chatService = chatService;
    }

    public AgentExecutor(String chatUserId, String appId, IApplicationChatService chatService) {
        this.chatUserId = chatUserId;
        this.appId = appId;
        this.chatService = chatService;
    }


    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> args = argumentsAsMap(toolExecutionRequest.arguments());
        String message = (String) args.getOrDefault("message","");
        ChatParams params = ChatParams.builder()
                .chatId("agent_"+ memoryId)
                .message(message)
                .reChat(false)
                .stream(false)
                .build();
        ChatState chatState = ChatState.builder()
                .appId(appId)
                .chatUserId(chatUserId)
                .chatUserType(ChatUserType.ANONYMOUS_USER)
                .source(ChatSource.ONLINE)
                .debug(false)
                .build();
        ChatResponse chatResponse = chatService.chatMessage(params, chatState, Sinks.many().unicast().onBackpressureBuffer());
        return chatResponse.getAnswer();
    }

}

