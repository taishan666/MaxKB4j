package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：工作流变量解析流程（global/chat/loop/node 作用域合并、引用字段查找）。
 */
class VariableResolverTest {

    private AbsNode newNode(String id, String nodeName) {
        JSONObject props = new JSONObject();
        if (nodeName != null) {
            props.put("nodeName", nodeName);
        }
        return new AbsNode(id, props) {};
    }

    @Test
    void getPromptVariables_mergesAllScopes() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");
        ctx.getChatContext().put("topic", "ai");
        ctx.getLoopContext().put("i", 0);

        AbsNode node = newNode("n1", "node1");
        node.getContext().put("answer", "hello");
        ctx.appendNode(node);

        VariableResolver resolver = new VariableResolver(ctx);
        Map<String, Object> vars = resolver.getPromptVariables();

        assertThat(vars.get("global.name")).isEqualTo("world");
        assertThat(vars.get("chat.topic")).isEqualTo("ai");
        assertThat(vars.get("loop.i")).isEqualTo(0);
        assertThat(vars.get("node1.answer")).isEqualTo("hello");
    }

    @Test
    void getPromptVariables_replacesNullNodeValueWithStar() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = newNode("n1", "node1");
        // 节点上下文为 LinkedHashMap（允许 null），解析时应把 null 统一转为 "*"
        node.getContext().put("empty", null);
        ctx.appendNode(node);
        VariableResolver resolver = new VariableResolver(ctx);
        assertThat(resolver.getPromptVariables().get("node1.empty")).isEqualTo("*");
    }

    @Test
    void getNodeVariables_returnsScopedByName() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode node = newNode("n1", "node1");
        node.getContext().put("answer", "hello");
        ctx.appendNode(node);

        VariableResolver resolver = new VariableResolver(ctx);
        Map<String, Object> vars = resolver.getNodeVariables(node);
        assertThat(vars).containsEntry("node1.answer", "hello");
    }

    @Test
    void getNodeVariables_handlesNullAndMissingName() {
        WorkflowContext ctx = new WorkflowContext();
        VariableResolver resolver = new VariableResolver(ctx);

        assertThat(resolver.getNodeVariables(null)).isEmpty();

        AbsNode noName = newNode("n2", null);
        assertThat(resolver.getNodeVariables(noName)).isEmpty();
    }

    @Test
    void getReferenceField_resolvesByScopeAndNode() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");

        AbsNode node = newNode("n1", "node1");
        node.getContext().put("answer", "hello");
        ctx.appendNode(node);

        VariableResolver resolver = new VariableResolver(ctx);
        assertThat(resolver.getReferenceField("global", "name")).isEqualTo("world");
        assertThat(resolver.getReferenceField("n1", "answer")).isEqualTo("hello");
        assertThat(resolver.getReferenceField(null, "x")).isNull();
        assertThat(resolver.getReferenceField("n1", null)).isNull();
        assertThat(resolver.getReferenceField("missing", "x")).isNull();
    }

    @Test
    void getFlowVariables_groupsByScopeKey() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");
        AbsNode node = newNode("n1", "node1");
        ctx.appendNode(node);

        Map<String, Map<String, Object>> flow = new VariableResolver(ctx).getFlowVariables();
        assertThat(flow).containsKeys("global", "chat", "loop", "n1");
        assertThat(flow.get("global").get("name")).isEqualTo("world");
    }
}