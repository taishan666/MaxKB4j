package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class NoModelParams implements ModelParams {

    @Override
    public List<BaseFiled> toForm() {
        return List.of();
    }
}
