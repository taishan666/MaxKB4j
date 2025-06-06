package com.tarzan.maxkb4j.core.workflow.node.mcp.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class McpParams {

    private String sseUrl;
    private String mcpTool;
    private JSONObject toolParams;
}
