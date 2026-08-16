package com.maxkb4j.tool.vo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ToolInputField;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ToolCardVO {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private String toolType;
    private JSONArray initFieldList;
    private JSONObject initParams;
    private List<ToolInputField> inputFieldList;
    private Boolean isActive;
    private String templateId;
    private String version;
    private Date createTime;
    private String nickname;
    private int resourceCount;
}
