package com.maxkb4j.model.dto;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class NotModelParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        return List.of();
    }
}
