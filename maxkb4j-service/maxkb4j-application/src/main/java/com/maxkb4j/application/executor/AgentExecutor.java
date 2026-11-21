package com.maxkb4j.application.executor;

import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.tool.executor.AbsToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.Map;

public class AgentExecutor extends AbsToolExecutor {

    /**
     * 子应用会话 id 前缀：chatId / chatUserId 均由父会话 memoryId 派生，跨请求稳定，
     * 缓存淘汰后可按相同 (chatId, appId, chatUserId) 从库中恢复子会话历史。
     */
    private static final String AGENT_CHAT_PREFIX = "agent_";

    private final String appId;
    private final IApplicationChatService chatService;

    public AgentExecutor(String appId, IApplicationChatService chatService) {
        this.appId = appId;
        this.chatService = chatService;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> args = argumentsAsMap(toolExecutionRequest.arguments());
        String message = (String) args.getOrDefault("message", "");
        String chatUserId = AGENT_CHAT_PREFIX + memoryId;
        // chatId 编入子应用 appId：同一父会话调用多个子应用时各自独立会话，避免互撞
        String chatId = chatUserId + "_" + appId;
        // chatMessage 仅认缓存或库中已初始化的会话，先补初始化：
        // 新会话写入缓存，已持久化的子会话从库中恢复历史记录
        chatService.getOrCreateChatInfo(chatId, appId, chatUserId);
        ChatParams params = ChatParams.builder()
                .chatId(chatId)
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
        ChatResponse chatResponse = chatService.chatMessage(params, chatState, null);
        return chatResponse.getAnswer();
    }

}
