package com.tarzan.maxkb4j.module.model.vo;

import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ModelVO extends ModelEntity {
    private String username;
}
