package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.core.form.BaseFiled;
import com.tarzan.maxkb4j.core.form.SingleSelectFiled;
import com.tarzan.maxkb4j.core.form.SliderFiled;
import com.tarzan.maxkb4j.module.model.provider.BaseModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SamBertTTSParams implements BaseModelParams {
    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> options=Map.of(
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
        BaseFiled voiceSelectFiled=new SingleSelectFiled("音色","voice","指定音色名称",options,"zhinan");
        BaseFiled volumeFiled=new SliderFiled(1,100,1,0,"音量","volume","指定音量，取值范围：0~100。",50);
        return List.of(voiceSelectFiled,volumeFiled);
    }
}
