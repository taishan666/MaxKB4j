package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.common.form.SliderFiled;
import com.tarzan.maxkb4j.module.model.provider.BaseModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CosyVoiceV1TTSParams implements BaseModelParams {
    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> options=Map.of(
                "龙婉","longwan",
                "龙橙","longcheng",
                "龙华","longhua",
                "龙小淳","longxiaochun",
                "龙小夏","longxiaoxia",
                "龙小诚","longxiaocheng",
                "龙小白","longxiaobai",
                "龙老铁","longlaotie",
                "龙书","longshu",
                "龙硕","longshuo"
        );
        BaseFiled voiceSelectFiled=new SingleSelectFiled("音色","voice","指定音色名称",options,"longxiaochun");
        BaseFiled volumeFiled=new SliderFiled(1,100,1,0,"音量","volume","指定音量，取值范围：0~100。",50);
        BaseFiled speechRateField=new SliderFiled(0.5F,2F,0.1F,1,"语速","speechRate","取值范围：0.5~2倍速。",1);
        return List.of(voiceSelectFiled,volumeFiled,speechRateField);
    }
}
