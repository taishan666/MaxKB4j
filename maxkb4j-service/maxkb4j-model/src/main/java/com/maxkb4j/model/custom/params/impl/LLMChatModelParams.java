package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SliderFiled;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class LLMChatModelParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        BaseField temperature=new SliderFiled(0.0F,1.5F,0.1F,0,"温度","temperature","较高的数值会使输出更加随机，而较低的数值会使其更加集中和确定",1F);
        BaseField maxTokens=new SliderFiled(1,1024000,1,0,"输出最大Tokens","maxTokens","指定模型可生成的最大token个数",1024);
        return List.of(temperature,maxTokens);
    }
}
