package com.tarzan.maxkb4j.module.application.wrokflow;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class Node {
    private String id;
    private String type;
    private Float x;
    private Float y;
    private JSONObject properties;
}
