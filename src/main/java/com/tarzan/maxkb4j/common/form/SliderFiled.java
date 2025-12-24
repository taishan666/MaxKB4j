package com.tarzan.maxkb4j.common.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SliderFiled extends BaseField {


    public SliderFiled(float min, float max, float step,int precision, String labelName,String field,String tooltip,float defaultValue) {
        super.setInput_type("Slider");
        JSONObject attrs =new JSONObject();
        attrs.put("min",min);
        attrs.put("max",max);
        attrs.put("step",step);
        attrs.put("precision",precision);
        attrs.put("show-input",true);
        attrs.put("show-input-controls",false);
        super.setAttrs(attrs);
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
