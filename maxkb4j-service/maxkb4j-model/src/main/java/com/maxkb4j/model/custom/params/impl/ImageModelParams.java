package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SingleSelectField;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ImageModelParams implements IModelParams {
    @Override
    public List<BaseField> toForm() {
        List<BaseField> fields=new ArrayList<>(1);
        Map<String,Object> options=getSizeOptions();
        if (!options.isEmpty()){
            Object defaultValue=options.values().stream().findFirst().get();
            BaseField size=new SingleSelectField("图片尺寸","size","生成图片的尺寸",getSizeOptions(),defaultValue);
            fields.add(size);
        }
        return fields;
    }

    public Map<String,Object> getSizeOptions(){
        return Map.of(
                "1024*1024","1024*1024",
                "720*1280","720*1280",
                "1280*720","1280*720"
        );
    }
}
