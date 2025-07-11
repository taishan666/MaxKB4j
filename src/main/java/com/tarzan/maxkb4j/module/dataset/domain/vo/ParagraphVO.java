package com.tarzan.maxkb4j.module.dataset.domain.vo;

import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphVO extends ParagraphEntity {
    private String datasetName;
    private String documentName;
    private String hitHandlingMethod;
    private Float similarity;
    private Float directlyReturnSimilarity;
    private Float comprehensiveScore;

   public boolean isHitHandlingMethod(){
       return "directly_return".equals(hitHandlingMethod)&& similarity>=directlyReturnSimilarity;
   }
}
