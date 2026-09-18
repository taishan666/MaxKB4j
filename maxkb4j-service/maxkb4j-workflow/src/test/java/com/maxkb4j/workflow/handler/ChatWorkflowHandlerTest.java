package com.maxkb4j.workflow.handler;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.model.IWorkflowOutputManager;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 聊天工作流处理器回归测试：类型令牌分发（仅 IChatWorkflow）、
 * 节点生命周期事件按聊天消息外发（开始/成功/错误帧）。
 */
class ChatWorkflowHandlerTest {

    private final IChatWorkflow workflow = mock(IChatWorkflow.class);
    private final IWorkflowOutputManager output = mock(IWorkflowOutputManager.class);
    private ChatWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatWorkflowHandler(mock(NodeCenter.class), new ExceptionResolverChain(List.of()));
        ChatParams chatParams = new ChatParams();
        chatParams.setChatId("chat-1");
        chatParams.setChatRecordId("record-1");
        when(workflow.getChatParams()).thenReturn(chatParams);
        when(workflow.output()).thenReturn(output);
    }

    private static AbsNode newNode() {
        JSONObject properties = new JSONObject();
        properties.put(RuntimeDetailField.NODE_NAME, "AI 节点");
        AbsNode node = new AbsNode("n1", properties) {
        };
        node.setType("ai-chat");
        return node;
    }

    @Test
    void canHandleShouldAcceptChatWorkflowsOnly() {
        assertThat(handler.canHandle(workflow)).isTrue();
        assertThat(handler.canHandle(mock(IKnowledgeWorkflow.class))).isFalse();
    }

    @Test
    void onNodeStartShouldEmitStartFrame() {
        AbsNode node = newNode();

        handler.onNodeStart(workflow, node);

        ArgumentCaptor<ChatMessageVO> captor = ArgumentCaptor.forClass(ChatMessageVO.class);
        verify(output).emit(captor.capture());
        assertThat(captor.getValue().getContent()).isEmpty();
        assertThat(captor.getValue().getNodeIsEnd()).isFalse();
    }

    @Test
    void onNodeSuccessShouldEmitEndFrameWithAnswer() {
        AbsNode node = newNode();
        node.setAnswerText("回答内容");

        handler.onNodeSuccess(workflow, node, null);

        ArgumentCaptor<ChatMessageVO> captor = ArgumentCaptor.forClass(ChatMessageVO.class);
        verify(output).emit(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("回答内容");
        assertThat(captor.getValue().getNodeIsEnd()).isTrue();
    }

    @Test
    void onNodeErrorShouldResolveMarkErrorAndEmitErrorFrame() {
        AbsNode node = newNode();

        handler.onNodeError(workflow, node, new IllegalStateException("boom"));

        assertThat(node.getStatus()).isEqualTo(NodeStatus.ERROR.getStatus());
        ArgumentCaptor<ChatMessageVO> captor = ArgumentCaptor.forClass(ChatMessageVO.class);
        verify(output, times(1)).emit(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Exception: boom");
        assertThat(captor.getValue().getNodeIsEnd()).isTrue();
    }
}
