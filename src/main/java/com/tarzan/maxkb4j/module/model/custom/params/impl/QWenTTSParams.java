package com.tarzan.maxkb4j.module.model.custom.params.impl;

import com.tarzan.maxkb4j.common.form.BaseField;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QWenTTSParams implements ModelParams {
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
