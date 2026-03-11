package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.INTENT_CLASSIFY;


@Slf4j
public class IntentClassifyNode extends AbsNode {


    public IntentClassifyNode(String id,JSONObject properties) {
        super(id,properties);
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
