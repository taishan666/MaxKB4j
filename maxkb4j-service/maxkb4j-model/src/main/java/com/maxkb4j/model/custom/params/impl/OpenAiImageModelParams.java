package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SingleSelectField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public  class OpenAiImageModelParams extends ImageModelParams {

    @Override
    public List<BaseField> toForm() {
        List<BaseField> fields=super.toForm();
        BaseField quality=new SingleSelectField("质量","quality","生成图像质量",getQualityOptions(),"hd");
        fields.add(quality);
        return fields;
    }

    @Override
    public Map<String,Object> getSizeOptions(){
        return Map.of(
                "1280x1280","1280x1280",
                "1568×1056","1568×1056",
                "1056×1568","1056×1568",
                "1472×1088","1472×1088",
                "1088×1472","1088×1472",
                "1728×960","1728×960",
                "960×1728","960×1728"
        );
    }

    public Map<String,Object> getQualityOptions(){
        return Map.of(
                "hd","hd",
                "standard","standard"
        );
    }
}
