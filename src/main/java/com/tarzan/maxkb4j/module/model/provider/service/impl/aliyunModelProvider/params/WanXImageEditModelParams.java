package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.common.form.SliderFiled;
import com.tarzan.maxkb4j.common.form.SwitchField;
import com.tarzan.maxkb4j.module.model.provider.dto.BaseModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis.ImageEditFunction.*;

@Data
public  class WanXImageEditModelParams implements BaseModelParams {

    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> functionOptions=Map.of(
                "全局风格化", STYLIZATION_ALL,
                "局部风格化", STYLIZATION_LOCAL,
                "指令编辑", DESCRIPTION_EDIT,
                "局部重绘", DESCRIPTION_EDIT_WITH_MASK,
                "去文字水印", "remove_watermark",
                "扩图", EXPAND,
                "图像超分", SUPER_RESOLUTION,
                "图像上色", COLORIZATION,
                "线稿生图", DOODLE,
                "参考卡通形象生图", "control_cartoon_feature"
        );
        BaseFiled functionSelectFiled=new SingleSelectFiled("功能","function","图像编辑功能",functionOptions,STYLIZATION_ALL);
                Map<String,Object> sizeOptions=Map.of(
                "1024*1024","1024*1024",
                "720*1280","720*1280",
                "1280*720","1280*720"
        );
        BaseFiled sizeSelectFiled=new SingleSelectFiled("图片尺寸","size","生成图片的尺寸",sizeOptions,"1024*1024");
        BaseFiled sliderFiled=new SliderFiled(1,4,1,0,"生成图片的数量","n","生成图片的数量。取值范围为1~4张",1);
        BaseFiled switchField=new SwitchField("提示词扩展","prompt_extend","提示词自动优化",false);
        return List.of(functionSelectFiled,sizeSelectFiled,sliderFiled,switchField);
    }
}

