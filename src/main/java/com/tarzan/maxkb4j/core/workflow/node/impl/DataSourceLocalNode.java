package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DATA_SOURCE_LOCAL;

public class DataSourceLocalNode extends INode {
    public DataSourceLocalNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(DATA_SOURCE_LOCAL.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {

    }

    @Data
    public static class NodeParams {
        private Integer fileSizeLimit;
        private Integer fileCountLimit;
        private List<String> fileTypeList;
    }
}
