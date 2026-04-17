package com.maxkb4j.model.custom.params.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CosyVoiceV1TTSParams extends TTSParams {

    @Override
    public Map<String,Object> getVoiceOptions(){
        return Map.of(
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
    }
}
