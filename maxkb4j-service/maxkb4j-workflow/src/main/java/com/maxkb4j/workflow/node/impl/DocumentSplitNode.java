package com.maxkb4j.workflow.node.impl;

import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;

@NodeCreatorType(NodeType.DOCUMENT_SPLIT)
public class DocumentSplitNode extends AbsNode {
    public DocumentSplitNode(String id, JSONObject properties) {
        super(id, properties);
    }

    @Data
    public static class NodeParams {
        private List<String> documentList;
        private String splitStrategy;
        private String[] patterns;
        private Integer chunkSize;
        private Boolean paragraphTitleRelateProblem;
        private Boolean documentNameRelateProblem;
        private Boolean withFilter;
    }
}
