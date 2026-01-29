package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DATA_SOURCE_WEB;

public class DataSourceWebNode extends AbsNode {
    public DataSourceWebNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(DATA_SOURCE_WEB.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
    }
}
