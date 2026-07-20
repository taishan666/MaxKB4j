package com.maxkb4j.model.vo;

import com.maxkb4j.model.entity.ModelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ModelVO extends ModelEntity {
    private String nickname;
}
