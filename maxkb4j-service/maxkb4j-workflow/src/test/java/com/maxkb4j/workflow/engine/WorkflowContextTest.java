package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.NodeReference;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：工作流上下文管理（节点追加/替换、执行节点查找、渲染与变量转发）。
 */
class WorkflowContextTest {

    private AbsNode newNode(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    @Test
    void appendNode_addsAndFindsNode() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode n1 = newNode("n1");
        ctx.appendNode(n1);
        assertThat(ctx.getExecutedNode("n1")).isSameAs(n1);
        assertThat(ctx.getExecutedNode("nope")).isNull();
    }

    @Test
    void appendNode_replacesSameRuntimeNodeInPlace() {
        WorkflowContext ctx = new WorkflowContext();
        AbsNode first = newNode("n1");
        first.getContext().put("answer", "old");
        ctx.appendNode(first);

        AbsNode second = newNode("n1"); // 相同 id + 空 upNodeIdList -> 相同 runtimeNodeId
        second.getContext().put("answer", "new");
        ctx.appendNode(second);

        AbsNode resolved = ctx.getExecutedNode("n1");
        assertThat(resolved).isSameAs(second);
        assertThat(resolved.getContext().get("answer")).isEqualTo("new");
        assertThat(ctx.getNodeContext()).hasSize(1);
    }

    @Test
    void render_delegatesToTemplateRenderer() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");
        assertThat(ctx.render("Hello {{global.name}}")).isEqualTo("Hello world");
        assertThat(ctx.getPromptVariables().get("global.name")).isEqualTo("world");
        assertThat(ctx.getReferenceField("global", "name")).isEqualTo("world");
    }
    @Test
    void getReferenceField_typedReference_resolvesGlobalScope() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");

        assertThat(ctx.getReferenceField(new NodeReference("global", "name"))).isEqualTo("world");
        assertThat(ctx.getReferenceField((NodeReference) null)).isNull();
    }

    @Test
    void getReferenceField_listCompatLayer_invalidReturnsNull() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");

        assertThat(ctx.getReferenceField(java.util.List.of("global", "name"))).isEqualTo("world");
        assertThat(ctx.getReferenceField(java.util.List.of("global"))).isNull();
        assertThat(ctx.getReferenceField((java.util.List<String>) null)).isNull();
    }

    @Test
    void getFieldValue_referenceSource_resolvesOrReturnsRawValue() {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", "world");

        assertThat(ctx.getFieldValue(java.util.List.of("global", "name"), "reference")).isEqualTo("world");
        assertThat(ctx.getFieldValue("raw", "reference")).isEqualTo("raw");
        assertThat(ctx.getFieldValue(java.util.List.of("global"), "reference")).isEqualTo(java.util.List.of("global"));
    }
}