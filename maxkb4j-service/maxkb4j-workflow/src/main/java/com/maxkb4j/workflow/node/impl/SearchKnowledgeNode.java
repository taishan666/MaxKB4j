package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.SEARCH_KNOWLEDGE)
public class SearchKnowledgeNode extends AbsNode {

    public SearchKnowledgeNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.PARAGRAPH_LIST, detail.get(NodeField.PARAGRAPH_LIST));
        context.put(NodeField.IS_HIT_HANDLING_METHOD_LIST, detail.get(NodeField.IS_HIT_HANDLING_METHOD_LIST));
        context.put(NodeField.DATA, detail.get(NodeField.DATA));
        context.put(NodeField.DIRECTLY_RETURN, detail.get(NodeField.DIRECTLY_RETURN));
    }

    @Data
    public static class NodeParams {
        private List<String> knowledgeIds;
        private KnowledgeSetting knowledgeSetting;
        private List<String> questionReferenceAddress;
        private String searchScopeType;
        private List<String> searchScopeReference;
        private Boolean showKnowledge;

    }

}
