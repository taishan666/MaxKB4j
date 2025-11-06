package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TextEmbeddingV4Params implements ModelParams {

    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> options=Map.of(
                "2,048",2048,
                "1,536",1536,
                "1,024",1024,
                "768",768,
                "512",512,
                "256",256,
                "128",128,
                "64",64
        );
        BaseFiled dimension=new SingleSelectFiled("dimension","dimension","向量维度",options,1024);
        return List.of(dimension);
    }
}
