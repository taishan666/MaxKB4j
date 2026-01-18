package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.RERANKER;

public class RerankerNode extends AbsNode {


    public RerankerNode(String id,JSONObject properties) {
        super(id,properties);
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
