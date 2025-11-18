package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class ApplicationNode extends INode {

    public ApplicationNode(JSONObject properties) {
        super(properties);
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
        private List<String> apiInputFieldList; // Optional

        private List<String> userInputFieldList; // Optional,

        private List<String> imageList; // Optional

        private List<String> documentList; // Optional

        private List<String> audioList; // Optional

        private List<String> otherList;

        private Boolean isResult;

        public Boolean getIsResult() {
            return isResult != null && isResult;
        }
    }


}