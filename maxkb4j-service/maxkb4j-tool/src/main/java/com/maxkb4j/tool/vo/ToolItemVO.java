package com.maxkb4j.tool.vo;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.ToolInputField;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ToolItemVO {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private String toolType;
    private Boolean isActive;
    private List<ToolInputField> inputFieldList;
    private JSONObject initParams;
    private Date createTime;
}
