package com.maxkb4j.workflow.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import com.maxkb4j.workflow.node.impl.DocumentExtractNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：文档解析节点上下文保存与恢复。
 */
class DocumentExtractNodeTest {

    /**
     * BUG-46 同类隐患回归：恢复执行时 detail 中的 content 为 JSONArray，
     * saveContext 不能强转 String[]，需逐元素重建后再拼接。
     */
    @Test
    void saveContext_withParsedJsonContent_joinsWithoutCastFailure() {
        DocumentExtractNode node = new DocumentExtractNode("doc1", new com.alibaba.fastjson.JSONObject());
        JSONArray content = JSON.parseArray("[\"第一段\",\"第二段\"]");
        Map<String, Object> detail = Map.of(
                NodeField.CONTENT, content,
                NodeField.DOCUMENT_LIST, List.of()
        );

        node.saveContext(null, detail);

        assertThat(node.getContext().get(NodeField.CONTENT))
                .isEqualTo("第一段" + DocumentExtractNode.SPLITTER + "第二段");
    }

    /**
     * 正常执行路径（content 为 String）行为不变。
     */
    @Test
    void saveContext_withLiveString_joinsWithoutChange() {
        DocumentExtractNode node = new DocumentExtractNode("doc1", new com.alibaba.fastjson.JSONObject());
        Map<String, Object> detail = Map.of(NodeField.CONTENT, "第一段", NodeField.DOCUMENT_LIST, List.of());

        node.saveContext(null, detail);

        assertThat(node.getContext().get(NodeField.CONTENT)).isEqualTo("第一段");
    }

    /**
     * getDetail 将拼接后的 content 拆回数组，往返一致。
     */
    @Test
    void getDetail_splitsJoinedContentBackToArray() {
        DocumentExtractNode node = new DocumentExtractNode("doc1", new com.alibaba.fastjson.JSONObject());
        Map<String, Object> detail = Map.of(
                NodeField.CONTENT, "第一段" + DocumentExtractNode.SPLITTER + "第二段",
                NodeField.DOCUMENT_LIST, List.of()
        );
        node.saveContext(null, detail);

        Map<String, Object> restored = node.getDetail();

        assertThat((String[]) restored.get(NodeField.CONTENT)).containsExactly("第一段", "第二段");
    }
}
