package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.common.form.SliderFiled;
import com.tarzan.maxkb4j.common.form.SwitchField;
import com.tarzan.maxkb4j.module.model.provider.BaseModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public  class WanXImageModelParams implements BaseModelParams {

    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> options=Map.of(
                "1024*1024","1024*1024",
                "720*1280","720*1280",
                "1280*720","1280*720"
        );
        BaseFiled sizeSelectFiled=new SingleSelectFiled("图片尺寸","size","生成图片的尺寸",options,"1024*1024");
        BaseFiled sliderFiled=new SliderFiled(1,4,1,0,"生成图片的数量","n","生成图片的数量。取值范围为1~4张",1);
        BaseFiled switchField=new SwitchField("提示词扩展","prompt_extend","提示词自动优化",false);
        return List.of(sizeSelectFiled,sliderFiled,switchField);
    }
}

