package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.common.form.SliderFiled;
import com.tarzan.maxkb4j.common.form.SwitchField;
import com.tarzan.maxkb4j.module.model.provider.dto.BaseModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public  class QwenImageModelParams implements BaseModelParams {

    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> sizeOptions=Map.of(
                "1328*1328(1:1)","1328*1328",
                "1664*928(16:9)","1664*928",
                "928*1664(9:16)","1928*1664",
                "1472*1140(4:3)","1472*1140",
                "1140*1472(3:4)","1140*1472"
        );
        BaseFiled size=new SingleSelectFiled("图片尺寸","size","生成图片的尺寸",sizeOptions,"1328*1328");
        BaseFiled n=new SliderFiled(1,4,1,0,"生成图片的数量","n","生成图片的数量。取值范围为1~4张",1);
        BaseFiled prompt_extend=new SwitchField("提示词扩展","prompt_extend","提示词自动优化",false);
        BaseFiled watermark=new SwitchField("水印","watermark","生成的图片带水印",false);
        return List.of(size,n,prompt_extend,watermark);
    }
}

