package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.RERANKER;


@NodeCreatorType(NodeType.RERANKER)
public class RerankerNode extends AbsNode {


    public RerankerNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(RERANKER.getKey());
    }


    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
        context.put("resultList", detail.get("resultList"));
    }

    @Data
    public static class NodeParams {
        private RerankerSetting rerankerSetting;
        private List<String> questionReferenceAddress;
        private String rerankerModelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private List<List<String>> rerankerReferenceList;
        private Boolean showKnowledge;
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
