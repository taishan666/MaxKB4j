package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SingleSelectField;
import com.maxkb4j.common.domain.form.SliderField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class TTSParams implements IModelParams {
    @Override
    public List<BaseField> toForm() {
        List<BaseField> fields=new ArrayList<>(3);
        Map<String,Object> options=getVoiceOptions();
        if (!options.isEmpty()){
            Object defaultValue=options.values().stream().findFirst().get();
            BaseField voiceSelectFiled=new SingleSelectField("音色","voice","指定音色名称",options,defaultValue);
            fields.add(voiceSelectFiled);
        }

        BaseField volumeFiled=new SliderField(1,100,1,0,"音量","volume","指定音量，取值范围：0~100。",50);
        fields.add(volumeFiled);
        BaseField speechRateField=new SliderField(0.5F,2F,0.1F,1,"语速","speechRate","取值范围：0.5~2倍速。",1);
        fields.add(speechRateField);
        return fields;
    }

    public Map<String,Object> getVoiceOptions(){
        return Map.of();
    }
}
