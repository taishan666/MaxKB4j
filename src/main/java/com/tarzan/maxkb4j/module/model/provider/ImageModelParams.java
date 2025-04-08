package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.form.BaseFiled;
import com.tarzan.maxkb4j.core.form.SingleSelectFiled;
import com.tarzan.maxkb4j.core.form.SliderFiled;
import com.tarzan.maxkb4j.core.form.SwitchField;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public  class ImageModelParams implements BaseModelParams{

    @Override
    public List<BaseFiled> toForm() {
        List<JSONObject> sizeOptionList=new ArrayList<>();
        sizeOptionList.add(new JSONObject().fluentPut("label","1024*1024").fluentPut("value","1024*1024"));
        sizeOptionList.add(new JSONObject().fluentPut("label","720*1280").fluentPut("value","720*1280"));
        sizeOptionList.add(new JSONObject().fluentPut("label","1280*720").fluentPut("value","1280*720"));
        BaseFiled sizeSelectFiled=new SingleSelectFiled("图片尺寸","size","生成图片的尺寸",sizeOptionList,"1024*1024");
        BaseFiled sliderFiled=new SliderFiled(1,4,1,0,"生成图片的数量","n","生成图片的数量。取值范围为1~4张",1);
        BaseFiled switchField=new SwitchField("提示词扩展","prompt_extend","提示词自动优化",false);
        return List.of(sizeSelectFiled,sliderFiled,switchField);
    }
}

