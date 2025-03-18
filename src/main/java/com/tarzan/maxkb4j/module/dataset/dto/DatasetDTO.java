package com.tarzan.maxkb4j.module.dataset.dto;

import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasetDTO extends DatasetEntity {
    private String sourceUrl;
    private String selector;
}
