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
        super.setViewType(ViewType.SINGLE_VIEW);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.BRANCH_NAME, detail.get(NodeField.BRANCH_NAME));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Answer> getAnswerList(String chatRecordId)  {
        String runtimeNodeId=this.getRuntimeNodeId();
        String formRender =buildFormRender(chatRecordId,runtimeNodeId,new JSONObject(detail));
        return List.of(Answer.builder().content(formRender).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(this.getViewType()).build());
    }


    /**
     * 组装以渲染标签包裹的表单设置字符串。
     */
    private String buildFormRender(String chatRecordId,String runtimeNodeId ,JSONObject nodeDetail) {
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, nodeDetail.getJSONArray(FormField.FORM_FIELD_LIST));
        formSetting.put(FormField.IS_SUBMIT, nodeDetail.getBooleanValue(FormField.IS_SUBMIT));
        formSetting.put(FormField.FORM_DATA, nodeDetail.getJSONObject(FormField.FORM_DATA));
        formSetting.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        formSetting.put(ChatField.CHAT_RECORD_ID, chatRecordId);
        return "<" + FormField.CARD_SELECTION_RENDER_TAG + ">" + formSetting + "</" + FormField.CARD_SELECTION_RENDER_TAG + ">";
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
