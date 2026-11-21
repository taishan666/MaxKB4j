package com.maxkb4j.application.executor;

import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BUG-35：AgentExecutor 以 "agent_"+memoryId 作 chatId 但从不调用 chatOpen 初始化会话，
 * chatMessage 缓存 miss 后查库必为空 → 必抛 common.record.not.exists。
 * 修复后 execute 前须先 getOrCreateChatInfo 初始化会话，且派生 id 跨请求稳定。
 */
@ExtendWith(MockitoExtension.class)
class AgentExecutorTest {

    private static final String APP_ID = "sub-app-1";
    private static final String PARENT_CHAT_ID = "parent-chat-42";
    private static final String AGENT_USER_ID = "agent_" + PARENT_CHAT_ID;
    private static final String AGENT_CHAT_ID = AGENT_USER_ID + "_" + APP_ID;

    @Mock
    private IApplicationChatService chatService;

    private AgentExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AgentExecutor(APP_ID, chatService);
    }

    private ToolExecutionRequest request(String message) {
        return ToolExecutionRequest.builder()
                .id("req-1")
                .name("agent_" + APP_ID)
                .arguments("{\"message\":\"" + message + "\"}")
                .build();
    }

    private ChatResponse response(String answer) {
        return new ChatResponse(List.of(Answer.builder().viewType("text").content(answer).build()), null);
    }

    @Test
    void execute_initializesSessionBeforeChat() {
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any()))
                .thenReturn(response("ok"));

        String answer = executor.execute(request("hi"), PARENT_CHAT_ID);

        assertEquals("ok", answer);
        // 会话初始化必须先于对话调用
        InOrder inOrder = inOrder(chatService);
        inOrder.verify(chatService).getOrCreateChatInfo(AGENT_CHAT_ID, APP_ID, AGENT_USER_ID);
        inOrder.verify(chatService).chatMessage(any(ChatParams.class), any(ChatState.class), isNull());
    }

    @Test
    void execute_derivesStableIdsAndParams() {
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any()))
                .thenReturn(response("done"));

        executor.execute(request("hi"), PARENT_CHAT_ID);

        ArgumentCaptor<ChatParams> paramsCaptor = ArgumentCaptor.forClass(ChatParams.class);
        ArgumentCaptor<ChatState> stateCaptor = ArgumentCaptor.forClass(ChatState.class);
        verify(chatService).chatMessage(paramsCaptor.capture(), stateCaptor.capture(), isNull());
        ChatParams params = paramsCaptor.getValue();
        // chatId 编入子应用 appId：同一父会话调用多个子应用时互不冲突
        assertEquals(AGENT_CHAT_ID, params.getChatId());
        assertEquals("hi", params.getMessage());
        assertFalse(params.getStream());
        assertFalse(params.getReChat());
        ChatState state = stateCaptor.getValue();
        assertEquals(APP_ID, state.getAppId());
        // chatUserId 由父会话 memoryId 派生，跨请求稳定（库恢复路径依赖它）
        assertEquals(AGENT_USER_ID, state.getChatUserId());
        assertEquals(ChatUserType.ANONYMOUS_USER, state.getChatUserType());
        assertEquals(ChatSource.ONLINE, state.getSource());
        assertFalse(state.getDebug());
    }
}
