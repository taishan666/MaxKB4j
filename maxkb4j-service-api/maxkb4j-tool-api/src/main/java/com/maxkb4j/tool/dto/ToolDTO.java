package com.maxkb4j.tool.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ToolInputField;
import lombok.Data;

import java.util.List;

@Data
public class ToolDTO {
    private String id;
    private String name;
    private String icon;
    private String desc;
    private String toolType;
    private String code;
    private List<ToolInputField> inputFieldList;
    private JSONArray initFieldList;
    private JSONObject initParams;
    private String userId;
    private Boolean isActive;
    private String label;
    private String scope;
    private String templateId;
    private String folderId;
    private String version;
}
