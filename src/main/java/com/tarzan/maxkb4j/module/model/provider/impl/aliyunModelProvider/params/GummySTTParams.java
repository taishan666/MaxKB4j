package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.params;

import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.form.SingleSelectFiled;
import com.tarzan.maxkb4j.module.model.provider.BaseModelParams;

import java.util.List;
import java.util.Map;

public class GummySTTParams implements BaseModelParams {
    @Override
    public List<BaseFiled> toForm() {
        Map<String,Object> options=Map.of(
                "无","none",
                "英文","en",
                "中文","zh",
                "日语","ja",
                "粤语","yue",
                "韩语","ko",
                "德语","de",
                "法语","fr",
                "俄语","ru",
                "意大利语","it"
        );
        BaseFiled voiceSelectFiled=new SingleSelectFiled("目标语言","targetLanguage","翻译语言",options,"none");
        return List.of(voiceSelectFiled);
    }
}
