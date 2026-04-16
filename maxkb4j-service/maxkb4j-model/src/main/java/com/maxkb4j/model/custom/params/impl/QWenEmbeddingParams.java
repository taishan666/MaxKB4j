package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SingleSelectFiled;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QWenEmbeddingParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        Map<String,Object> options=Map.of(
                "1,024",1024,
                "768",768,
                "512",512
        );
        BaseField dimension=new SingleSelectFiled("dimension","dimension","向量维度",options,1024);
        return List.of(dimension);
    }
}
