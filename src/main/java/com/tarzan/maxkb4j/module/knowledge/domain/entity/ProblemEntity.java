package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.domain.base.entity.BaseEntity;
import lombok.*;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("problem")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemEntity extends BaseEntity {
    
    private String content;
    private Integer hitNum;
    private String knowledgeId;

    public static ProblemEntity createDefault() {
        ProblemEntity entity= new ProblemEntity();
        entity.setHitNum(0);
        return entity;
    }
}
