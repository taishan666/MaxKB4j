package com.tarzan.maxkb4j.common.form;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RadioCardFiled extends BaseFiled{

    public RadioCardFiled(String labelName, String field, Map<String,Object> options, Object defaultValue) {
        super.setInput_type("RadioCard");
        List<JSONObject> optionList=new ArrayList<>();
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            JSONObject option=new JSONObject();
            option.put("label",entry.getKey());
            option.put("value",entry.getValue());
            optionList.add(option);
        }
        super.setOption_list(optionList);
        super.setLabel(labelName);
        super.setField(field);
        super.setText_field("label");
        super.setValue_field("value");
        super.setRequired(true);
        super.setDefault_value(defaultValue);
        super.setShow_default_value(true);
        super.setAttrs(new JSONObject());
    }

    public RadioCardFiled(String labelName, String field, Map<String,Object> options) {
        super.setInput_type("RadioCard");
        List<JSONObject> optionList=new ArrayList<>();
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            JSONObject option=new JSONObject();
            option.put("label",entry.getKey());
            option.put("value",entry.getValue());
            optionList.add(option);
        }
        super.setOption_list(optionList);
        super.setLabel(labelName);
        super.setField(field);
        super.setText_field("label");
        super.setValue_field("value");
        super.setRequired(true);
        super.setShow_default_value(false);
        super.setAttrs(new JSONObject());
    }
}
