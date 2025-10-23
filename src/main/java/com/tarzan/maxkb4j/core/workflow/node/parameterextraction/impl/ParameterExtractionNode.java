package com.tarzan.maxkb4j.core.workflow.node.parameterextraction.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.Data;

import java.util.List;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.PARAMETER_EXTRACTION;

public class ParameterExtractionNode extends INode {

    public ParameterExtractionNode(JSONObject properties) {
        super(properties);
        super.setType(PARAMETER_EXTRACTION.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        private String modelId;
        private JSONObject modelParamsSetting;
        private List<String> inputVariable;
        private List<Field> variableList;

    }

    @Data
    public static class Field {
        private String field;
        private String label;
        private String parameterType;
        private String desc;
    }
}
