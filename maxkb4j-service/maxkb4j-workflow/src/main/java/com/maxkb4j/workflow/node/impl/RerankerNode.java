package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.RERANKER)
public class RerankerNode extends AbsNode {

    public RerankerNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.RESULT, detail.get(NodeField.RESULT));
        context.put(NodeField.RESULT_LIST, detail.get(NodeField.RESULT_LIST));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private RerankerSetting rerankerSetting;
        private List<String> questionReferenceAddress;
        private String rerankerModelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private List<List<String>> rerankerReferenceList;
        private Boolean showKnowledge;

        @Override
        public String getModelId() {
            return rerankerModelId;
        }

        @Override
        public JSONObject getModelParamsSetting() {
            return null;
        }
    }

    @Data
    public static class RerankerSetting {
        private Integer topN;
        private Float similarity;
        private Integer maxParagraphCharNumber;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class RerankResult {
        private String pageContent;
        private Map<String,Object> metadata;
    }

}
