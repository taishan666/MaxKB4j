package com.maxkb4j.model.custom.params.impl;

import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.form.SingleSelectFiled;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QWenTTSParams implements IModelParams {
    @Override
    public List<BaseField> toForm() {
        Map<String,Object> options=Map.of(
                "Cherry","CHERRY",
                "Serena","SERENA",
                "Ethan","ETHAN",
                "Chelsie","CHELSIE",
                "Dylan","DYLAN",
                "Jada","JADA",
                "Sunny","SUNNY",
                "Nofish","NOFISH",
                "Jennifer","JENNIFER",
                "Li","LI"
        );
        BaseField voiceSelectFiled=new SingleSelectFiled("音色","voice","指定音色名称",options,"CHERRY");
        return List.of(voiceSelectFiled);
    }
}
