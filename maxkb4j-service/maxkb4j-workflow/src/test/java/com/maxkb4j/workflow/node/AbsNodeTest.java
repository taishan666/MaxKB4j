package com.maxkb4j.workflow.node;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.enums.NodeStatus;
import org.junit.jupiter.api.Test;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.ViewType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：节点生命周期与 CAS 抢占流程（防止菱形汇聚时重复执行）。
 */
class AbsNodeTest {

    private AbsNode newNode(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    @Test
    void freshNodeIsReadyAndClaimsOnce() {
        AbsNode node = newNode("n1");
        assertThat(node.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
        assertThat(node.tryClaimRunning()).isTrue();
        assertThat(node.getStatus()).isEqualTo(NodeStatus.STARTED.getStatus());
        // 已被抢占，再次抢占失败
        assertThat(node.tryClaimRunning()).isFalse();
    }

    @Test
    void tryClaimRunning_returnsFalseForTerminalStates() {
        AbsNode success = newNode("n1");
        success.setStatus(NodeStatus.SUCCESS.getStatus());
        assertThat(success.tryClaimRunning()).isFalse();

        AbsNode error = newNode("n2");
        error.setStatus(NodeStatus.ERROR.getStatus());
        assertThat(error.tryClaimRunning()).isFalse();
    }

    @Test
    void interruptStateCanBeClaimed() {
        AbsNode node = newNode("n1");
        node.setStatus(NodeStatus.INTERRUPT.getStatus());
        assertThat(node.tryClaimRunning()).isTrue();
        assertThat(node.getStatus()).isEqualTo(NodeStatus.STARTED.getStatus());
    }

    @Test
    void getNodeData_returnsNodeDataWhenPresent() {
        JSONObject props = new JSONObject();
        JSONObject nodeData = new JSONObject();
        nodeData.put("config", "value");
        props.put("nodeData", nodeData);
        AbsNode node = new AbsNode("n1", props) {};

        assertThat(node.getNodeData().getString("config")).isEqualTo("value");
    }

    @Test
    void getNodeData_returnsEmptyWhenAbsent() {
        AbsNode node = newNode("n1");
        assertThat(node.getNodeData().isEmpty()).isTrue();
    }

    @Test
    void getAnswerList_emptyWhenNoAnswerText() {
        AbsNode node = newNode("n1");
        assertThat(node.getAnswerList()).isEmpty();
    }

    @Test
    void getAnswerList_buildsAnswerWithContentAndReasoning() {
        // isResult 是 getAnswerList 输出答案的前置条件（运行时处理器写入节点 detail）
        AbsNode node = newNode("n1");
        node.getDetail().put(NodeField.IS_RESULT, true);
        node.getDetail().put(NodeField.REASONING_CONTENT_ENABLE, true);
        // getAnswerList 从节点 detail 读取 ANSWER / REASONING_CONTENT 键
        node.getDetail().put(NodeField.ANSWER, "done");
        node.getDetail().put(NodeField.REASONING_CONTENT, "because");
        node.setViewType(ViewType.SINGLE_VIEW);

        java.util.List<Answer> answers = node.getAnswerList();
        assertThat(answers).hasSize(1);
        Answer answer = answers.get(0);
        assertThat(answer.getContent()).isEqualTo("done");
        assertThat(answer.getReasoningContent()).isEqualTo("because");
        assertThat(answer.getChatRecordId()).isEmpty();
        assertThat(answer.getViewType()).isEqualTo(ViewType.SINGLE_VIEW);
    }
}