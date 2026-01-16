package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.MCP;

public class McpNode extends AbsNode {

    public McpNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(MCP.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        private JSONArray mcpTools;
        private String mcpTool;
        private String mcpSource;
        private JSONObject toolParams;
        private String mcpServers;
        private String mcpToolId;
    }

}
