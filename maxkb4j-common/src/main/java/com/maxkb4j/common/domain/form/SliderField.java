package com.maxkb4j.common.domain.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SliderField extends BaseField {

    private String trigger_type;
    public SliderField(float min, float max, float step, int precision, String labelName, String field, String tooltip, float defaultValue) {
        super("Slider",labelName,field,tooltip,true,defaultValue);
        JSONObject attrs =new JSONObject();
        attrs.put("min",min);
        attrs.put("max",max);
        attrs.put("step",step);
        attrs.put("precision",precision);
        attrs.put("show-input",true);
        attrs.put("show-input-controls",false);
        super.setAttrs(attrs);
        this.setTrigger_type("OPTION_LIST");
    }
}
