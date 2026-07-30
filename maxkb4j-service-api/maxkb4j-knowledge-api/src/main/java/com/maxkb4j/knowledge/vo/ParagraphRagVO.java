package com.maxkb4j.knowledge.vo;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.RagContent;
import com.maxkb4j.knowledge.consts.HitHandlingMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphRagVO extends RagContent {
    private String id;
    private String title;
    private String content;
    private Integer position;
    private Boolean isActive;
    private Double similarity;
    private String hitHandlingMethod;
    private Double directlyReturnSimilarity;
    private String documentName;
    private JSONObject meta;
    private String knowledgeName;
    private Integer knowledgeType;

    public boolean shouldDirectlyReturn() {
        return HitHandlingMethod.HIT_HANDLING_DIRECTLY_RETURN.equals(hitHandlingMethod)
                && similarity != null
                && directlyReturnSimilarity != null
                && similarity >= directlyReturnSimilarity;
    }
}
