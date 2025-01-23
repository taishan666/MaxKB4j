package com.tarzan.maxkb4j.module.model.provider.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ModelInputVO {
    private JSONObject attrs;
    private Object default_value;
    private String field;
    private String input_type;
    private Object label;
    private JSONObject props_info;
    private JSONObject relation_show_field_dict;
    private JSONObject relation_trigger_field_dict;
    private Boolean required;
    private String trigger_type;

    public ModelInputVO() {
        this.attrs=new JSONObject();
        this.props_info=new JSONObject();
        this.relation_show_field_dict=new JSONObject();
        this.relation_trigger_field_dict=new JSONObject();
        this.required=true;
        this.trigger_type="OPTION_LIST";
    }
}
