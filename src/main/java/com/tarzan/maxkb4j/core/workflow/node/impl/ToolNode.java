package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TOOL;

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
