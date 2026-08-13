package com.maxkb4j.tool.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.entity.ToolInputField;
import lombok.Data;

import java.util.List;

/**
 * 工具创建/更新入参：仅包含客户端可提供的字段；
 * userId / scope / isActive / toolType 等由服务端按场景强制设置。
 *
 * @author tarzan
 */
@Data
public class ToolSaveDTO {

    private String name;

    private String desc;

    private String code;

    private List<ToolInputField> inputFieldList;

    private JSONArray initFieldList;

    private JSONObject initParams;

    private String label;

    private String icon;

    private String version;

    private String folderId;

    private String templateId;

    /** 工具类型（CUSTOM / HTTP / MCP / SKILL 等），创建时未提供则默认 CUSTOM；更新时为空表示不变。 */
    private String toolType;
}
