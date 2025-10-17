package com.tarzan.maxkb4j.module.model.provider.dto;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import lombok.Data;

import java.util.List;

@Data
public  class NoModelParams implements BaseModelParams{

    @Override
    public List<BaseFiled> toForm() {
        return List.of();
    }
}
