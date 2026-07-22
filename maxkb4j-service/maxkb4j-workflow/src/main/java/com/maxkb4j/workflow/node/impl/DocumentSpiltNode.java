package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;

import static com.maxkb4j.workflow.enums.NodeType.DOCUMENT_SPLIT;


@NodeCreatorType(NodeType.DOCUMENT_SPLIT)
public class DocumentSpiltNode extends AbsNode {
    public DocumentSpiltNode(String id, JSONObject properties) {
        super(id, properties);
        super.setType(DOCUMENT_SPLIT.getKey());
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
