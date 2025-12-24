package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.form.BaseField;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TextEmbeddingV3Params implements ModelParams {

    @Override
    public List<BaseField> toForm() {
        Map<String,Object> options=Map.of(
                "1,024",1024,
                "768",768,
                "512",512,
                "256",256,
                "128",128,
                "64",64
        );
        BaseField dimension=new SingleSelectFiled("dimension","dimension","向量维度",options,1024);
        return List.of(dimension);
    }
}
