package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.domain.form.BaseField;
import com.tarzan.maxkb4j.common.domain.form.SliderFiled;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class LlmModelParams implements ModelParams {

    @Override
    public List<BaseField> toForm() {
        BaseField sliderFiled=new SliderFiled(0.1f,1,0.01f,2,"温度","temperature","较高的数值会使输出更加随机，而较低的数值会使其更加集中和确定",0.7f);
        BaseField sliderFiled1=new SliderFiled(1,100000,1,0,"输出最大Tokens","maxTokens","指定模型可生成的最大token个数",800);
       // BaseFiled switchField=new SwitchField("是否为多模态模型","isMultimodalModel","是否为多模态模型",false);
        return List.of(sliderFiled,sliderFiled1);
    }
}
