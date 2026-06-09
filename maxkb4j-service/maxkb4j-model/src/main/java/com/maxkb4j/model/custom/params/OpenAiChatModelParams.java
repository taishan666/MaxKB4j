package com.maxkb4j.model.custom.params;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SliderField;
import com.maxkb4j.common.domain.form.SwitchField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;

@Data
public  class OpenAiChatModelParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        BaseField temperature=new SliderField(0.0F,1.5F,0.1F,0,"温度","temperature","较高的数值会使输出更加随机，而较低的数值会使其更加集中和确定",1F);
        BaseField maxTokens=new SliderField(1,1024000,1,0,"输出最大Tokens","max_tokens","指定模型可生成的最大token个数",1024);
        BaseField switchField=new SwitchField("思考模式","enable_thinking","在输出最终回答之前，模型会先输出一段思维链内容，以提升最终答案的准确性。",true);
        return List.of(temperature,maxTokens,switchField);
    }
}
