package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.USER_SELECT)
public class UserSelectNode extends AbsNode {
    public UserSelectNode(String id,JSONObject properties) {
        super(id,properties);
        super.setViewType("single_view");
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.BRANCH_NAME, detail.get(NodeField.BRANCH_NAME));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Answer> getAnswerList(String chatRecordId)  {
        Map<String, Object> formData = (Map<String, Object>) context.getOrDefault(FormField.FORM_DATA,Map.of());
        boolean isSubmit = (boolean) context.getOrDefault(FormField.IS_SUBMIT,false);
        String runtimeNodeId=this.getRuntimeNodeId();
        JSONArray formFieldList =new JSONArray();
        if (!formData.isEmpty()){
             formFieldList =(JSONArray)context.getOrDefault(FormField.FORM_FIELD_LIST, new JSONArray());
        }
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, formFieldList);
        formSetting.put(FormField.IS_SUBMIT, isSubmit);
        formSetting.put(FormField.FORM_DATA, formData);
        formSetting.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        formSetting.put("chatRecordId", chatRecordId);
        String formRender = "<card_selection_render>" + formSetting + "</card_selection_render>";
        return List.of(Answer.builder().content(formRender).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(this.getViewType()).build());
    }

    @Data
    public static class NodeParams {
        private List<Branch> branch;
        private JSONObject formData;
        private String labelName;
    }

    @Data
    public static class Branch {
        private String id;
        private String option;
    }

}
