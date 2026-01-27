package com.tarzan.maxkb4j.common.form;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SingleSelectFiled extends BaseField {


    public SingleSelectFiled(String labelName, String field, String tooltip, Map<String,Object> options, Object defaultValue) {
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
        List<JSONObject> optionList=new ArrayList<>();
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            JSONObject option=new JSONObject();
            option.put("label",entry.getKey());
            option.put("value",entry.getValue());
            optionList.add(option);
        }
        super.setProps_info(new JSONObject());
        super.setRelation_show_field_dict(new JSONObject());
        super.setRelation_trigger_field_dict(new JSONObject());
        super.setOption_list(optionList);
        super.setLabel(label);
        super.setField(field);
        super.setText_field("label");
        super.setValue_field("value");
        super.setTrigger_type("OPTION_LIST");
        super.setRequired(true);
        super.setDefaultValue(defaultValue);
        super.setShow_default_value(true);
    }
}
