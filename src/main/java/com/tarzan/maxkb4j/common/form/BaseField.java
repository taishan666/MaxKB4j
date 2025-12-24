package com.tarzan.maxkb4j.common.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class BaseField {
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
    private List<JSONObject> option_list;
    private String text_field;
    private String value_field;
    private Boolean show_default_value;
}
