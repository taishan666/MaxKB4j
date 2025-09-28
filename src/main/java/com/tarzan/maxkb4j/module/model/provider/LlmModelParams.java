package com.tarzan.maxkb4j.module.model.provider;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SliderFiled;
import lombok.Data;

import java.util.List;

@Data
public  class LlmModelParams implements BaseModelParams{

    @Override
    public List<BaseFiled> toForm() {
        BaseFiled sliderFiled=new SliderFiled(0.1f,1,0.01f,2,"温度","temperature","较高的数值会使输出更加随机，而较低的数值会使其更加集中和确定",0.7f);
        BaseFiled sliderFiled1=new SliderFiled(1,100000,1,0,"输出最大Tokens","maxTokens","指定模型可生成的最大token个数",800);
        return List.of(sliderFiled,sliderFiled1);
    }
}
