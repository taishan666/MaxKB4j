package com.tarzan.maxkb4j.module.application.workflow.info;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class Node {
    private String id;
    private String type;
    private Integer x;
    private Integer y;
    private JSONObject properties;
}
