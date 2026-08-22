package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.FormRenderUtil;
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
    public List<Answer> getAnswerList(String chatRecordId)  {
        String runtimeNodeId=this.getRuntimeNodeId();
        String formRender = FormRenderUtil.buildFormRender(chatRecordId, runtimeNodeId, new JSONObject(detail), FormField.CARD_SELECTION_RENDER_TAG);
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
