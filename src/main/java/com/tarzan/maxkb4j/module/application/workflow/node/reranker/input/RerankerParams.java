package com.tarzan.maxkb4j.module.application.workflow.node.reranker.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class RerankerParams extends BaseParams {
    @JsonProperty("reranker_setting")
    private RerankerSetting rerankerSetting;
    @JsonProperty("question_reference_address")
    private List<String> questionReferenceAddress;
    @JsonProperty("reranker_model_id")
    private String rerankerModelId;
    @JsonProperty("reranker_reference_list")
    private List<List<String>> rerankerReferenceList;
}
