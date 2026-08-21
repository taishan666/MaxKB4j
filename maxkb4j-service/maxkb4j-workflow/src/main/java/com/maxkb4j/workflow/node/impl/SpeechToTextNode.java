package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.SPEECH_TO_TEXT)
public class SpeechToTextNode extends AbsNode {

    public SpeechToTextNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.RESULT, detail.get(NodeField.RESULT));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String sttModelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private JSONObject modelParamsSetting;
        private List<String> audioList;
        private Boolean isResult;

        @Override
        public String getModelId() {
            return sttModelId;
        }
    }
}
