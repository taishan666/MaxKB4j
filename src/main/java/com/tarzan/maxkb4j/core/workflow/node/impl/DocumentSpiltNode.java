package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DOCUMENT_SPLIT;

public class DocumentSpiltNode extends AbsNode {
    public DocumentSpiltNode(String id, JSONObject properties) {
        super(id, properties);
        super.setType(DOCUMENT_SPLIT.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
    }

    @Data
    public static class NodeParams  {
        private List<String> documentList;
        private String splitStrategy;
        private String[]  patterns;
        private Integer chunkSize;
        private Boolean paragraphTitleRelateProblem;
        private Boolean documentNameRelateProblem;
        private Boolean withFilter;
    }
}
