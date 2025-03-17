package com.tarzan.maxkb4j.module.application.workflow.node.reranker.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class RerankerParams extends BaseParams {
    private RerankerSetting rerankerSetting;
    private List<String> questionReferenceAddress;
    private String rerankerModelId;
    private List<List<String>> rerankerReferenceList;
}
