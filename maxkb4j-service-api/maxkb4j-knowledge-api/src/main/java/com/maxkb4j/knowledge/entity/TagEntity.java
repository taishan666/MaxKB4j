package com.maxkb4j.knowledge.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName(value = "tag")
public class TagEntity extends BaseEntity {
    private String name;
    private String knowledgeId;
}
