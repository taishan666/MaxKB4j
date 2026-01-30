package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.domain.form.BaseField;
import com.tarzan.maxkb4j.common.domain.form.SingleSelectFiled;
import com.tarzan.maxkb4j.common.domain.form.SliderFiled;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CosyVoiceV2TTSParams implements ModelParams {
    @Override
    public List<BaseField> toForm() {
        Map<String,Object> options=Map.of(
                "龙婉","longwan_v2",
                "龙橙","longcheng_v2",
                "龙华","longhua_v2",
                "龙小淳","longxiaochun_v2",
                "龙小夏","longxiaoxia_v2",
                "龙小诚","longxiaocheng_v2",
                "龙小白","longxiaobai_v2",
                "龙老铁","longlaotie_v2",
                "龙书","longshu_v2",
                "龙硕","longshuo_v2"
        );
        BaseField voiceSelectFiled=new SingleSelectFiled("音色","voice","指定音色名称",options,"longxiaochun_v2");
        BaseField volumeFiled=new SliderFiled(1,100,1,0,"音量","volume","指定音量，取值范围：0~100。",50);
        BaseField speechRateField=new SliderFiled(0.5F,2F,0.1F,1,"语速","speechRate","取值范围：0.5~2倍速。",1);
        return List.of(voiceSelectFiled,volumeFiled,speechRateField);
    }
}
