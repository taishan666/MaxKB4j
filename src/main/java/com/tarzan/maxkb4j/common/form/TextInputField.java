package com.tarzan.maxkb4j.common.form;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public class TextInputField extends BaseField {
    public TextInputField(String labelName,String field,Boolean required) {
        super.setInput_type("TextInput");
        super.setLabel(labelName);
        super.setField(field);
        super.setRequired(required);
    }

    public TextInputField(String labelName,String field,String placeholder,Boolean required) {
        super.setInput_type("TextInput");
        super.setLabel(labelName);
        super.setField(field);
        super.setRequired(required);
        super.setAttrs(new JSONObject(Map.of("placeholder",placeholder)));

    }

    public TextInputField(String labelName,String field,Boolean required,Object defaultValue) {
        super.setInput_type("TextInput");
        super.setLabel(labelName);
        super.setField(field);
        super.setRequired(required);
        super.setDefault_value(defaultValue);
        if (defaultValue != null){
            super.setShow_default_value(true);
        }
    }
}
