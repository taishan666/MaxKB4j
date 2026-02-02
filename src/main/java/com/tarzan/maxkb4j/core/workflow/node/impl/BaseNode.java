package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.BASE;

public class BaseNode extends AbsNode {


    public BaseNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(BASE.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {

    }


}
