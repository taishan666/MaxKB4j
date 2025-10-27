package com.tarzan.maxkb4j.core.workflow.node.reranker.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.RERANKER;

public class RerankerNode extends INode {


    public RerankerNode(JSONObject properties) {
        super(properties);
        this.setType(RERANKER.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
        context.put("resultList", detail.get("resultList"));
    }

    @Data
    public static class NodeParams {
        private RerankerSetting rerankerSetting;
        private List<String> questionReferenceAddress;
        private String rerankerModelId;
        private List<List<String>> rerankerReferenceList;
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
