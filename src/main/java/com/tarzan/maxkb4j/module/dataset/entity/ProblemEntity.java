package com.tarzan.maxkb4j.module.dataset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("problem")
@NoArgsConstructor
public class ProblemEntity extends BaseEntity {
    
    private String content;
    private Integer hitNum;
    private String datasetId;

    public static ProblemEntity createDefault() {
        ProblemEntity entity= new ProblemEntity();
        entity.setHitNum(0);
        return entity;
    }
}
