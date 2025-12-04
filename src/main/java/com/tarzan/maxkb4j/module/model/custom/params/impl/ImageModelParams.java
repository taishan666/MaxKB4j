package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.common.form.SwitchField;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ImageModelParams implements ModelParams {
    @Override
    public List<BaseFiled> toForm() {
        //支持在 [512, 1440] 像素范围内任意组合宽高，总像素不超过 1440*1440
        Map<String,Object> sizeOptions=Map.of(
                "1024*1024","1024*1024",
                "720*1280","720*1280",
                "1280*720","1280*720"
        );
        BaseFiled size=new SingleSelectFiled("图片尺寸","size","生成图片的尺寸",sizeOptions,"1024*1024");
        BaseFiled watermark=new SwitchField("水印","watermark","生成的图片带水印",false);
        return List.of(size,watermark);
    }
}
