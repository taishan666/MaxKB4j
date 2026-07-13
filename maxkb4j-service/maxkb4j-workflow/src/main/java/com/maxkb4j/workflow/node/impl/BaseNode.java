package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.BASE;


public class BaseNode extends AbsNode {


    public BaseNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(BASE.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {

    }


}
