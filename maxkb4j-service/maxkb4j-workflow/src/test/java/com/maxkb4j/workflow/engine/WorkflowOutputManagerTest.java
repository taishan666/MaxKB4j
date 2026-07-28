package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 回归测试:工作流输出管理器(响应式输出判定、答案收集、运行时详情聚合、Sink 安全发射)。
 */
class WorkflowOutputManagerTest {

    private AbsNode newNode(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    private AbsNode newNode(String id, String nodeName) {
        JSONObject props = new JSONObject();
        props.put("nodeName", nodeName);
        AbsNode node = new AbsNode(id, props) {};
        node.setType("ai-chat");
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        return node;
    }

    private WorkflowConfiguration config(WorkflowMode mode, List<AbsNode> nodes, String chatRecordId) {
        WorkflowConfiguration configuration = new WorkflowConfiguration(mode, nodes, null);
        if (chatRecordId != null) {
            configuration.setChatParams(ChatParams.builder().chatRecordId(chatRecordId).build());
        }
        return configuration;
    }

    @Test
    void needsSink_trueForApplicationModes() {
        WorkflowContext ctx = new WorkflowContext();
        assertThat(output(WorkflowMode.APPLICATION, ctx).needsSink()).isTrue();
        assertThat(output(WorkflowMode.APPLICATION_LOOP, ctx).needsSink()).isTrue();
    }

    @Test
    void needsSink_falseForKnowledgeModes() {
        WorkflowContext ctx = new WorkflowContext();
        assertThat(output(WorkflowMode.KNOWLEDGE, ctx).needsSink()).isFalse();
        assertThat(output(WorkflowMode.KNOWLEDGE_LOOP, ctx).needsSink()).isFalse();
    }

    @Test
    void answers_emptyWhenNoConfiguredNodes() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = newNode("n1");
        node.setAnswerText("hello");
        ctx.appendNode(node);
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(), "rec-1");

        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, null);

        assertThat(manager.answers()).isEmpty();
    }

    @Test
    void answers_emptyWhenChatRecordIdNull() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = newNode("n1");
        node.setAnswerText("hello");
        ctx.appendNode(node);
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(node), null);

        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, null);

        assertThat(manager.answers()).isEmpty();
    }

    @Test
    void answers_collectsOnlyFromConfiguredNodesWithAnswer() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode configured = newNode("n1");
        configured.setAnswerText("configured-answer");
        ctx.appendNode(configured);
        AbsNode notConfigured = newNode("n2");
        notConfigured.setAnswerText("orphan-answer");
        ctx.appendNode(notConfigured);
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(configured), "rec-1");

        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, null);

        List<Answer> answers = manager.answers();
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getContent()).isEqualTo("configured-answer");
        assertThat(answers.get(0).getChatRecordId()).isEqualTo("rec-1");
    }

    @Test
    void emit_whenNeedsSink_emitsToSink() {
        Sinks.Many<ChatMessageVO> sink = mock();
        WorkflowContext ctx = new WorkflowContext();
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(), "rec-1");
        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, sink);
        ChatMessageVO message = new ChatMessageVO("chat-1", "rec-1", true);

        manager.emit(message);

        verify(sink).tryEmitNext(message);
    }

    @Test
    void emit_whenNotNeedsSink_skipsEmit() {
        Sinks.Many<ChatMessageVO> sink = mock();
        WorkflowContext ctx = new WorkflowContext();
        WorkflowConfiguration configuration = config(WorkflowMode.KNOWLEDGE, List.of(), "rec-1");
        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, sink);
        ChatMessageVO message = new ChatMessageVO("chat-1", "rec-1", true);

        manager.emit(message);

        verify(sink, never()).tryEmitNext(any());
    }

    @Test
    void emit_nullMessage_skipsSafely() {
        Sinks.Many<ChatMessageVO> sink = mock();
        WorkflowContext ctx = new WorkflowContext();
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(), "rec-1");
        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, sink);

        manager.emit(null);

        verify(sink, never()).tryEmitNext(any());
    }

    @Test
    void runtimeDetails_buildsPerNodeDetail() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = newNode("n1", "My Node");
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        node.setErrMessage("");
        node.setUpNodeIdList(List.of("start"));
        node.getDetail().put("answer", "hi");
        ctx.appendNode(node);
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(node), "rec-1");

        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, null);
        JSONObject details = manager.runtimeDetails();

        assertThat(details).hasSize(1);
        JSONObject detail = details.getJSONObject(node.getRuntimeNodeId());
        assertThat(detail).isNotNull();
        assertThat(detail.getIntValue("index")).isEqualTo(0);
        assertThat(detail.getString("nodeId")).isEqualTo("n1");
        assertThat(detail.getString("name")).isEqualTo("My Node");
        assertThat(detail.getString("type")).isEqualTo("ai-chat");
        assertThat(detail.getIntValue("status")).isEqualTo(NodeStatus.SUCCESS.getStatus());
        assertThat(detail.getString("errMessage")).isEqualTo("");
        assertThat(detail.getJSONArray("upNodeIdList")).containsExactly("start");
        assertThat(detail.getString("runtimeNodeId")).isEqualTo(node.getRuntimeNodeId());
        assertThat(detail.getString("answer")).isEqualTo("hi");
    }

    @Test
    void runtimeDetails_nameFallsBackToTypeWhenPropertiesNull() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = new AbsNode("n1", null) {};
        node.setType("question-classifier");
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        ctx.appendNode(node);
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(node), "rec-1");

        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, null);
        JSONObject details = manager.runtimeDetails();

        JSONObject detail = details.getJSONObject(node.getRuntimeNodeId());
        assertThat(detail.getString("name")).isEqualTo("question-classifier");
    }

    @Test
    void runtimeDetails_emptyWhenNoValidNodes() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = newNode("n1");
        ctx.appendNode(node);
        WorkflowConfiguration configuration = config(WorkflowMode.APPLICATION, List.of(), "rec-1");

        WorkflowOutputManager manager = new WorkflowOutputManager(configuration, ctx, null);
        assertThat(manager.runtimeDetails()).isEmpty();
    }

    private WorkflowOutputManager output(WorkflowMode mode, WorkflowContext ctx) {
        WorkflowConfiguration configuration = new WorkflowConfiguration(mode, List.of(), null);
        return new WorkflowOutputManager(configuration, ctx, null);
    }
}