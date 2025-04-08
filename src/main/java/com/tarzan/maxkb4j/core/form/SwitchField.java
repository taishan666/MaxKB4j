package com.tarzan.maxkb4j.core.form;

import com.alibaba.fastjson.JSONObject;

public class SwitchField extends BaseFiled{

    public SwitchField(String labelName,String field,String tooltip,boolean defaultValue) {
        super.setInput_type("SwitchInput");
        super.setAttrs(new JSONObject());
        JSONObject label=new JSONObject();
        JSONObject labelAttrs=new JSONObject();
        labelAttrs.put("tooltip",tooltip);
        label.put("attrs",labelAttrs);
        label.put("input_type","TooltipLabel");
        label.put("label",labelName);
        label.put("props_info",new JSONObject());
        super.setLabel(label);
        super.setField(field);
        super.setDefault_value(defaultValue);
        super.setShow_default_value(true);
    }
}
