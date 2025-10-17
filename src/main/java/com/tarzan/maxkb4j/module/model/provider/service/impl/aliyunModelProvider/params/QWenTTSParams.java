package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.module.model.provider.dto.BaseModelParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QWenTTSParams implements BaseModelParams {
    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> options=Map.of(
                "Cherry","CHERRY",
                "Serena","SERENA",
                "Ethan","ETHAN",
                "Chelsie","CHELSIE"
        );
        BaseFiled voiceSelectFiled=new SingleSelectFiled("音色","voice","指定音色名称",options,"CHERRY");
        return List.of(voiceSelectFiled);
    }
}
