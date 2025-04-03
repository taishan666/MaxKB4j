package com.tarzan.maxkb4j.core.workflow.node.reranker.input;

import lombok.Data;

import java.util.List;

@Data
public class RerankerParams {
    private RerankerSetting rerankerSetting;
    private List<String> questionReferenceAddress;
    private String rerankerModelId;
    private List<List<String>> rerankerReferenceList;
}
