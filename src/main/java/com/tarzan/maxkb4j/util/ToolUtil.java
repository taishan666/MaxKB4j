package com.tarzan.maxkb4j.util;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.service.ToolService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolExecutor;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class ToolUtil {

    private final ToolService toolService;

    public Map<ToolSpecification, ToolExecutor> getTools(List<String> toolIds) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        if (CollectionUtils.isEmpty(toolIds)) {
            return tools;
        }
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.in(ToolEntity::getId, toolIds);
        wrapper.eq(ToolEntity::getIsActive, true);
        List<ToolEntity> toolEntities = toolService.list(wrapper);
        for (ToolEntity tool : toolEntities) {
            if ("MCP".equals(tool.getToolType())) {
                JSONObject jsonObject = JSONObject.parseObject(tool.getCode());
                jsonObject.keySet().forEach(key -> {
                    JSONObject serverConfig = (JSONObject) jsonObject.get(key);
                    McpTransport transport;
                    if ("sse".equals(serverConfig.getString("type"))) {
                        transport = new HttpMcpTransport.Builder()
                                .sseUrl("https://mcp.api-inference.modelscope.net/d08b5b22651144/sse")
                                .logRequests(true) // if you want to see the traffic in the log
                                .logResponses(true)
                                .build();
                    } else {
                        transport = new StreamableHttpMcpTransport.Builder()
                                .url("https://mcp.api-inference.modelscope.net/d08b5b22651144/sse")
                                .logRequests(true) // if you want to see the traffic in the log
                                .logResponses(true)
                                .build();
                    }
                    McpClient mcpClient = new DefaultMcpClient.Builder()
                            .key("MaxKB4JMCPClient")
                            .transport(transport)
                            .build();
                    Map<ToolSpecification, ToolExecutor> mcpTools = mcpClient.listTools().stream().collect(Collectors.toMap(
                            mcpTool -> mcpTool,
                            mcpTool -> new McpToolExecutor(mcpClient)
                    ));
                    tools.putAll(mcpTools);
                });
            } else {
                List<ToolInputField> params = tool.getInputFieldList();
                JsonObjectSchema.Builder parametersBuilder = JsonObjectSchema.builder();
                for (ToolInputField param : params) {
                    JsonSchemaElement jsonSchemaElement = new JsonNullSchema();
                    if ("string".equals(param.getType())) {
                        jsonSchemaElement = JsonStringSchema.builder().build();
                    } else if ("int".equals(param.getType())) {
                        jsonSchemaElement = JsonIntegerSchema.builder().build();
                    } else if ("number".equals(param.getType())) {
                        jsonSchemaElement = JsonNumberSchema.builder().build();
                    } else if ("boolean".equals(param.getType())) {
                        jsonSchemaElement = JsonBooleanSchema.builder().build();
                    } else if ("array".equals(param.getType())) {
                        jsonSchemaElement = JsonArraySchema.builder().build();
                    } else if ("object".equals(param.getType())) {
                        jsonSchemaElement = JsonObjectSchema.builder().build();
                    }
                    parametersBuilder.addProperty(param.getName(), jsonSchemaElement);
                }
                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name(tool.getName())
                        .description(tool.getDesc())
                        .parameters(parametersBuilder.build())
                        .build();
                tools.put(toolSpecification, new GroovyScriptExecutor(tool.getCode()));
            }
        }
        return tools;
    }
}
