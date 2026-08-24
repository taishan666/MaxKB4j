package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@Slf4j
@NodeCreatorType(NodeType.AI_CHAT)
public class AiChatNode extends AbsNode {

    public AiChatNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.ANSWER, detail.get(NodeField.ANSWER));
        context.put(NodeField.REASONING_CONTENT, detail.get(NodeField.REASONING_CONTENT));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String modelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private String system;
        private String prompt;
        private String dialogueType;
        private int dialogueNumber;
        private Boolean isResult;
        private JSONObject modelParamsSetting;
        private JSONObject modelSetting;
        private Boolean toolOutputEnable;
        private List<String> toolIds;
        private List<String> applicationIds;
        private List<String> imageList;

    }

}
