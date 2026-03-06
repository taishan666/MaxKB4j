package com.tarzan.maxkb4j.module.tool.executor;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.ToolExecutionResult;
import reactor.core.publisher.Sinks;

import java.util.Map;

public class AgentExecutor extends AbsToolExecutor{

    private final String appId;
    private final ApplicationChatService chatService;

    public AgentExecutor(String appId, ApplicationChatService chatService) {
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

    @Override
    public ToolExecutionResult executeWithContext(ToolExecutionRequest request, InvocationContext context) {
        return super.executeWithContext(request, context);
    }
}

