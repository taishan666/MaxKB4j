package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Answer;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

public class UserSelectNode extends AbsNode {
    public UserSelectNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(USER_SELECT.getKey());
        super.setViewType("single_view");
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("branchName", detail.get("branchName"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Answer> getAnswerList()  {
        Map<String, Object> formData = (Map<String, Object>) context.getOrDefault("form_data",Map.of());
        boolean isSubmit = (boolean) context.getOrDefault("is_submit",false);
        String runtimeNodeId=this.getRuntimeNodeId();
        JSONArray formFieldList =(JSONArray)context.getOrDefault("form_field_list", new JSONArray());
        JSONObject formSetting = new JSONObject();
        formSetting.put("form_field_list", formFieldList);
        formSetting.put("is_submit", isSubmit);
        formSetting.put("form_data", formData);
        formSetting.put("runtimeNodeId", runtimeNodeId);
        String formRender = "<card_selection_render>" + formSetting + "</card_selection_render>";
        return List.of(Answer.builder().content(formRender).reasoningContent("").runtimeNodeId(runtimeNodeId).viewType(this.getViewType()).build());

    }

    @Data
    public static class NodeParams {
        private List<Branch> branch;
        private JSONObject formData;
        private String labelName;
        private Boolean isResult;
    }

    @Data
    public static class Branch {
        private String id;
        private String type;
        private String option;
    }

}
