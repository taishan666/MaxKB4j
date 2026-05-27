package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SliderField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;

@Data
public class OllamaImageModelParams implements IModelParams {
    @Override
    public List<BaseField> toForm() {
        BaseField steps=new SliderField(1,100,1,0,"迭代步数","steps","生成图片的数量。取值范围为1~4张",20);
        BaseField seed=new SliderField(1,10000,1,0,"随机种子","seed","生成图片的数量。取值范围为1~4张",42);
        return List.of(steps,seed);
    }


}
