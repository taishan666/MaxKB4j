package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SliderFiled;
import com.maxkb4j.common.domain.form.SwitchField;
import com.maxkb4j.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class LlmModelParams implements ModelParams {

    @Override
    public List<BaseField> toForm() {
        BaseField sliderFiled=new SliderFiled(1,100000,1,0,"输出最大Tokens","maxTokens","指定模型可生成的最大token个数",800);
        BaseField switchField=new SwitchField("是否为多模态模型","isMultimodalModel","是否为多模态模型",false);
        return List.of(sliderFiled,switchField);
    }
}
