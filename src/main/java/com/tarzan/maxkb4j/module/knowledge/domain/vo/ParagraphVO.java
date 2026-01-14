package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.tarzan.maxkb4j.common.typehandler.JSONBTypeHandler;
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
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    private Float comprehensiveScore;

   public boolean isHitHandlingMethod(){
       return "directlyReturn".equals(hitHandlingMethod)&& similarity>=directlyReturnSimilarity;
   }
}
