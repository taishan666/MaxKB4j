package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SliderFiled;
import com.maxkb4j.common.domain.form.SwitchField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class OLlamaChatModelParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        BaseField temperature=new SliderFiled(0.0F,1.5F,0.1F,0,"温度","temperature","较高的数值会使输出更加随机，而较低的数值会使其更加集中和确定",1F);
        BaseField switchField=new SwitchField("是否返回思考","returnThinking","是否返回思考输出",true);
        return List.of(temperature,switchField);
    }
}
