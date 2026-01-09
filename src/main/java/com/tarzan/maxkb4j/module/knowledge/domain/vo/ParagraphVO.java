package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphVO extends ParagraphEntity {
    private String knowledgeName;
    private Integer knowledgeType;
    private String documentName;
    private String hitHandlingMethod;
    private Float similarity;
    private Float directlyReturnSimilarity;
    private Float comprehensiveScore;

   public boolean isHitHandlingMethod(){
       return "directlyReturn".equals(hitHandlingMethod)&& similarity>=directlyReturnSimilarity;
   }
}
