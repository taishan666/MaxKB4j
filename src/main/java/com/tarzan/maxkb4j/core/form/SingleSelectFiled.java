package com.tarzan.maxkb4j.core.form;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class SingleSelectFiled extends BaseFiled{


    public SingleSelectFiled(String labelName, String field, String tooltip,List<JSONObject> optionList, Object defaultValue) {
        super.setInput_type("SingleSelect");
        JSONObject attrs =new JSONObject();
        super.setAttrs(attrs);
        JSONObject label=new JSONObject();
        JSONObject labelAttrs=new JSONObject();
        labelAttrs.put("tooltip",tooltip);
        label.put("attrs",labelAttrs);
        label.put("input_type","TooltipLabel");
        label.put("label",labelName);
        label.put("props_info",new JSONObject());
        super.setOption_list(optionList);
        super.setLabel(label);
        super.setField(field);
        super.setText_field("label");
        super.setValue_field("value");
        super.setTrigger_type("OPTION_LIST");
        super.setRequired(true);
        super.setDefault_value(defaultValue);
    }
}
