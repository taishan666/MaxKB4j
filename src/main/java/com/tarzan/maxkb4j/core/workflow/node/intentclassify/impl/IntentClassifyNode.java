package com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.INTENT_CLASSIFY;

@Slf4j
public class IntentClassifyNode extends INode {


    public IntentClassifyNode(JSONObject properties) {
        super(properties);
        super.setType(INTENT_CLASSIFY.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

    @Data
    public static class NodeParams {

        private String modelId;
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
