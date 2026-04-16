package com.maxkb4j.model.custom.params.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SamBertTTSParams extends TTSParams {

    @Override
    public Map<String,Object> getVoiceOptions(){
       return Map.of(
                "知楠","zhinan",
                "知琪","zhiqi",
                "知厨","zhichu",
                "知德","zhide",
                "知佳","zhijia",
                "知茹","zhiru",
                "知倩","zhiqian",
                "知祥","zhixiang",
                "知薇","zhiwei",
                "知浩","zhihao"
        );
    }
}
