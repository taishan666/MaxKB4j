package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.DocumentEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocumentVO extends DocumentEntity {
    private Integer paragraphCount;
}
