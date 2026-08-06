package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.InputField;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.APPLICATION)
public class ApplicationNode extends AbsNode {

    public ApplicationNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        @NotBlank(message = "应用图标")
        private String icon;
        @NotBlank(message = "应用名称")
        private String name;
        @NotBlank(message = "应用id不能为空")
        private String applicationId;
        @NotNull(message = "用户问题不能为空")
        private List<String> questionReferenceAddress;
        //api 输入
        private List<InputField> apiInputFieldList;
        //用户输入
        private List<InputField> userInputFieldList;
        private List<String> imageList;
        private List<String> documentList;
        private List<String> audioList;
        private List<String> otherList;
        private Boolean isResult;

    }

}