package com.tarzan.maxkb4j.core.workflow.node.mcp.input;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class McpParams {

    private JSONArray mcpTools;
    private String mcpTool;
    private String mcpSource;
    private JSONObject toolParams;
    private String mcpServers;
    private String mcpToolId;
}
