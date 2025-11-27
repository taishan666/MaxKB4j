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
                    if ("string".equals(param.getType())) {
                        parametersBuilder.addStringProperty(param.getName());
                    } else if ("int".equals(param.getType())) {
                        parametersBuilder.addIntegerProperty(param.getName());
                    } else if ("number".equals(param.getType())) {
                        parametersBuilder.addNumberProperty(param.getName());
                    } else if ("boolean".equals(param.getType())) {
                        parametersBuilder.addBooleanProperty(param.getName());
                    } else if ("array".equals(param.getType())) {
                        JsonSchemaElement element = JsonArraySchema.builder().build();
                        parametersBuilder.addProperty(param.getName(), element);
                    } else if ("object".equals(param.getType())) {
                        JsonSchemaElement element = JsonObjectSchema.builder().build();
                        parametersBuilder.addProperty(param.getName(),element);
                    }
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
