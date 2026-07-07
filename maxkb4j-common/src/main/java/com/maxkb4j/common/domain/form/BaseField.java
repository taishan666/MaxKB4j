package com.maxkb4j.common.domain.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@Data
public class BaseField {
    private JSONObject attrs;
    private Object default_value;
    private String field;
    private String input_type;
    private Object label;
    private Boolean required;
    private Boolean show_default_value;

    public BaseField(String inputType,String labelName,String field,String tooltip,boolean required,Object defaultValue) {
        this.setInput_type(inputType);
        if (StringUtils.isNotBlank(tooltip)){
            JSONObject label=new JSONObject();
            JSONObject labelAttrs=new JSONObject();
            labelAttrs.put("tooltip",tooltip);
            label.put("attrs",labelAttrs);
            label.put("input_type","TooltipLabel");
            label.put("label",labelName);
            label.put("props_info",new JSONObject());
            this.setLabel(label);
        }else {
            this.setLabel(labelName);
        }
        this.setAttrs(new JSONObject());
        this.setField(field);
        this.setShow_default_value(required);
        this.setRequired(required);
        this.setDefault_value(defaultValue);
    }
}
