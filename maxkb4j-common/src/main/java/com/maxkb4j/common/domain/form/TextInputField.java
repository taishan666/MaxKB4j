package com.maxkb4j.common.domain.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class TextInputField extends BaseField {

    public TextInputField(String labelName,String field,String placeholder,Boolean required) {
        super("TextInput",labelName,field,"",required,null);
        super.setAttrs(new JSONObject(Map.of("placeholder",placeholder)));

    }

    public TextInputField(String labelName,String field,Boolean required,Object defaultValue) {
        super("TextInput",labelName,field,"",required,defaultValue);
    }
}
