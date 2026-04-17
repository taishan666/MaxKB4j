package com.maxkb4j.model.custom.params.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CosyVoiceV2TTSParams extends TTSParams {

    @Override
    public Map<String,Object> getVoiceOptions(){
        return Map.of(
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
    }
}
