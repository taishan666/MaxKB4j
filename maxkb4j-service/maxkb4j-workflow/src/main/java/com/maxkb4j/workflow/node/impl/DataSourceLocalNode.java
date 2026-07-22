package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;

import static com.maxkb4j.workflow.enums.NodeType.DATA_SOURCE_LOCAL;

@NodeCreatorType(NodeType.DATA_SOURCE_LOCAL)
public class DataSourceLocalNode extends AbsNode {
    public DataSourceLocalNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(DATA_SOURCE_LOCAL.getKey());
    }

    @Data
    public static class NodeParams {
        private Integer fileSizeLimit;
        private Integer fileCountLimit;
        private List<String> fileTypeList;
    }
}
