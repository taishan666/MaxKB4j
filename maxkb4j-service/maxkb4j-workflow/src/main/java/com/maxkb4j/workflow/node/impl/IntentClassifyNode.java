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

@Slf4j
@NodeCreatorType(NodeType.INTENT_CLASSIFY)
public class IntentClassifyNode extends AbsNode {

    public IntentClassifyNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String modelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private JSONObject modelParamsSetting;
        private List<String> contentList;
        private int dialogueNumber;
        private List<Branch> branch;
    }

    @Data
    public static class Branch {
        private String id;
        //private Boolean isOther;
        private String content;
    }

}
