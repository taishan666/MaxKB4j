package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.NodeField;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.APPLICATION;


public class ApplicationNode extends AbsNode {

    public ApplicationNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(APPLICATION.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
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
        private List<NodeField> apiInputFieldList;
        //用户输入
        private List<NodeField> userInputFieldList;
        private List<String> imageList;
        private List<String> documentList;
        private List<String> audioList;
        private List<String> otherList;
        private Boolean isResult;

    }


}