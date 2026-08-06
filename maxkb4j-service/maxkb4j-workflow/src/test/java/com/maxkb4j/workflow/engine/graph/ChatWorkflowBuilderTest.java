package com.maxkb4j.workflow.engine.graph;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 回归测试：ChatWorkflowBuilder 构建流程与空安全。
 */
class ChatWorkflowBuilderTest {

    private AbsNode node(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    private ChatState chatStateWithDetails() {
        ChatRecordDTO chatRecord = new ChatRecordDTO();
        chatRecord.setDetails(new JSONObject());
        return ChatState.builder().chatRecord(chatRecord).build();
    }

    @Test
    void build_withNullChatParamsAndDetailsInChatState_doesNotThrow() {
        // 回归：旧实现 chatParams 为 null 时在 build() 内直接解引用导致 NPE
        assertThatCode(() -> ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, List.of(node("n1")), List.of())
                .chatState(chatStateWithDetails())
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    void build_withMinimalParameters_returnsUsableWorkflow() {
        ChatWorkflow workflow = ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, List.of(node("n1")), List.of())
                .build();

        assertThat(workflow).isNotNull();
        assertThat(workflow.getNode("n1")).isNotNull();
        assertThat(workflow.getHistoryChatRecords()).isEmpty();
        assertThat(workflow.getGlobalContext()).isNotNull();
        assertThat(workflow.context()).isNotNull();
        assertThat(workflow.execution()).isNotNull();
        assertThat(workflow.output()).isNotNull();
    }

    @Test
    void build_returnsChatWorkflowWithParamsAndState() {
        ChatParams chatParams = ChatParams.builder().message("hi").chatId("c1").build();
        ChatState chatState = ChatState.builder().build();

        ChatWorkflow workflow = ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, List.of(node("n1")), List.of())
                .chatParams(chatParams)
                .chatState(chatState)
                .build();

        assertThat(workflow.getChatParams()).isSameAs(chatParams);
        assertThat(workflow.getChatState()).isSameAs(chatState);
    }

    @Test
    void restoreState_isFluentAndTogglesFlag() {
        ChatWorkflowBuilder builder = ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, List.of(), List.of());

        assertThat(builder.restoreState(new JSONObject(), "runtimeNodeId", null)).isSameAs(builder);
    }
}
