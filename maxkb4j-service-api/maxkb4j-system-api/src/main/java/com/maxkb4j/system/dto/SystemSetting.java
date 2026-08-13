package com.maxkb4j.system.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SystemSetting {
    private Integer type;
    private JSONObject meta;
}
