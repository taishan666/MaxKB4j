package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ToolInputField;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.TOOL;


public class ToolNode extends AbsNode {
    public ToolNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(TOOL.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        private List<ToolInputField> inputFieldList;
        private String code;
        private Map<String,Object> initParams;
        private Boolean isResult;
    }
}
