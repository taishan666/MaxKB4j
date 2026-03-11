package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SingleSelectFiled;
import com.maxkb4j.common.domain.form.SliderFiled;
import com.maxkb4j.common.domain.form.SwitchField;
import com.maxkb4j.model.service.ModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public  class WanXImageModelParams implements ModelParams {

    @Override
    public List<BaseField> toForm() {
        //支持在 [512, 1440] 像素范围内任意组合宽高，总像素不超过 1440*1440
        Map<String,Object> sizeOptions=Map.of(
                "1024*1024","1024*1024",
                "720*1280","720*1280",
                "1280*720","1280*720"
        );
        BaseField size=new SingleSelectFiled("图片尺寸","size","生成图片的尺寸",sizeOptions,"1024*1024");
        BaseField n=new SliderFiled(1,4,1,0,"生成图片的数量","n","生成图片的数量。取值范围为1~4张",1);
        BaseField prompt_extend=new SwitchField("提示词扩展","prompt_extend","提示词自动优化",false);
        BaseField watermark=new SwitchField("水印","watermark","生成的图片带水印",false);
        return List.of(size,n,prompt_extend,watermark);
    }
}

