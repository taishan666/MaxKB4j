package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;

@Slf4j
public class AiChatNode extends AbsNode {


    public AiChatNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(AI_CHAT.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }


    @Data
    public static class NodeParams {
        private String modelId;
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
