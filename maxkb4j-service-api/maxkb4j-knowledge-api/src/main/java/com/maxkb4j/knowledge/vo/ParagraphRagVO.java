package com.maxkb4j.knowledge.vo;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maxkb4j.common.domain.RagContent;
import com.maxkb4j.knowledge.consts.HitHandlingMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphRagVO extends RagContent {
    private String id;
    private Boolean isActive;
    private Double similarity;
    @JsonIgnore
    private String hitHandlingMethod;
    @JsonIgnore
    private Double directlyReturnSimilarity;
    private String documentId;
    private JSONObject meta;
    private String knowledgeId;
    private String knowledgeName;
    private Integer knowledgeType;

    public boolean returnIfSatisfied() {
        return HitHandlingMethod.HIT_HANDLING_DIRECTLY_RETURN.equals(hitHandlingMethod)
                && similarity != null
                && directlyReturnSimilarity != null
                && similarity >= directlyReturnSimilarity;
    }
}
