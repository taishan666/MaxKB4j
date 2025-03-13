package com.tarzan.maxkb4j.module.dataset.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphVO extends ParagraphEntity {
    private String datasetName;
    private String documentName;
    private String hitHandlingMethod;
    private Double similarity;
    private Double directlyReturnSimilarity;
    private Double comprehensiveScore;

   public boolean isHitHandlingMethod(){
       return "directly_return".equals(hitHandlingMethod)&& similarity>=directlyReturnSimilarity;
   }
}
