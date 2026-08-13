package com.maxkb4j.model.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SingleSelectField extends BaseField {
    private List<JSONObject> option_list;
    private String text_field;
    private String value_field;

    public SingleSelectField(String labelName, String field, String tooltip, Map<String,Object> options, Object defaultValue) {
        super("SingleSelect",labelName,field,tooltip,true,defaultValue);
        List<JSONObject> optionList=new ArrayList<>();
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            JSONObject option=new JSONObject();
            option.put("label",entry.getKey());
            option.put("value",entry.getValue());
            optionList.add(option);
        }
        this.setOption_list(optionList);
        this.setText_field("label");
        this.setValue_field("value");
    }
}
