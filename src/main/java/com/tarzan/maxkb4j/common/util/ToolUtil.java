package com.tarzan.maxkb4j.common.util;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.service.ToolService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class ToolUtil {

    private final ToolService toolService;

    public Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds) {
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
                JSONObject mcpServers = JSONObject.parseObject(tool.getCode());
                tools.putAll(McpToolUtil.getToolMap(mcpServers));
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
