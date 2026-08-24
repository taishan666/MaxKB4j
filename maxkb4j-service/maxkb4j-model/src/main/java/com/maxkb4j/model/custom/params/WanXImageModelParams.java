package com.maxkb4j.model.custom.params;

import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.form.SingleSelectField;
import com.maxkb4j.model.form.SliderField;
import com.maxkb4j.model.form.SwitchField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

@Data
public  class WanXImageModelParams implements IModelParams {

    @Override
    public List<BaseField> toForm() {
        //支持在 [512, 1440] 像素范围内任意组合宽高，总像素不超过 1440*1440
        Map<String,Object> sizeOptions=Map.of(
                "1024*1024","1024*1024",
                "720*1280","720*1280",
                "1280*720","1280*720"
        );
        BaseField size=new SingleSelectField("图片尺寸",ParamKey.SIZE,"生成图片的尺寸",sizeOptions,"1024*1024");
        BaseField n=new SliderField(1,4,1,0,"生成图片的数量",ParamKey.N,"生成图片的数量。取值范围为1~4张",1);
        BaseField prompt_extend=new SwitchField("提示词扩展",ParamKey.PROMPT_EXTEND,"提示词自动优化",false);
        BaseField watermark=new SwitchField("水印",ParamKey.WATERMARK,"生成的图片带水印",false);
        return List.of(size,n,prompt_extend,watermark);
    }
}

