package com.tarzan.maxkb4j.module.dataset.domain.vo;

import com.tarzan.maxkb4j.module.dataset.domain.entity.DocumentEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocumentVO extends DocumentEntity {
    private Integer paragraphCount;
}
