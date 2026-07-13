package com.maxkb4j.knowledge.vo;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParagraphVO extends ParagraphEntity {
    private String knowledgeName;
    private Integer knowledgeType;
    private String documentName;
    private String hitHandlingMethod;
    private Double similarity;
    private Double directlyReturnSimilarity;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    private Double comprehensiveScore;

    /**
     * 是否应“直接返回”：命中处理方式为 {@link DocumentEntity#HIT_HANDLING_DIRECTLY_RETURN}
     * 且相似度达到 {@link #directlyReturnSimilarity} 阈值。
     *
     * <p>原方法名 {@code isHitHandlingMethod} 易被误读为字段 getter；重命名为 {@code shouldDirectlyReturn}
     * 以表达“判定谓词”语义。同时对 {@code similarity}/{@code directlyReturnSimilarity} 为 null 做兜底，避免拆箱 NPE。</p>
     */
    public boolean shouldDirectlyReturn() {
        return DocumentEntity.HIT_HANDLING_DIRECTLY_RETURN.equals(hitHandlingMethod)
                && similarity != null
                && directlyReturnSimilarity != null
                && similarity >= directlyReturnSimilarity;
    }
}
