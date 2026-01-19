package com.tarzan.maxkb4j.module.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.util.GroovyScriptExecutor;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.enums.ToolType;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ToolUtilService {

    private final ToolService toolService;
    private final ApplicationService applicationService;
    private final ApplicationApiKeyService apiKeyService;

    @Value("${server.port}")
    private int serverPort;

    public Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds, List<String> applicationIds) {
        Map<ToolSpecification, ToolExecutor> tools = getToolMap(toolIds);
        if (!CollectionUtils.isEmpty(applicationIds)) {
            JSONObject mcpServers = new JSONObject();
            LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery();
            wrapper.select(ApplicationEntity::getId, ApplicationEntity::getName);
            wrapper.in(ApplicationEntity::getId, applicationIds);
            List<ApplicationEntity> applications = applicationService.list(wrapper);
            for (ApplicationEntity app : applications) {
                JSONObject mcpConfig = getAppMcpConfig(app.getId());
                mcpServers.put(app.getName(), mcpConfig);
            }
            tools.putAll(McpToolUtil.getToolMap(mcpServers));
        }
        return tools;
    }

    public JSONObject getAppMcpConfig(String appId) {
        ApplicationApiKeyEntity apiKey = apiKeyService.lambdaQuery().select(ApplicationApiKeyEntity::getSecretKey).eq(ApplicationApiKeyEntity::getApplicationId, appId).last("limit 1").one();
        JSONObject mcpConfig = new JSONObject();
        mcpConfig.put("url", "http://127.0.0.1:" + serverPort + "/chat/api/mcp");
        mcpConfig.put("type", "streamable_http");
        mcpConfig.put("headers", Map.of("Authorization", "Bearer " + apiKey.getSecretKey()));
        return mcpConfig;
    }


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
            if (ToolType.MCP.getValue().equals(tool.getToolType())) {
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
                        parametersBuilder.addProperty(param.getName(), element);
                    }
                }
                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name(tool.getName())
                        .description(tool.getDesc())
                        .parameters(parametersBuilder.build())
                        .build();
                tools.put(toolSpecification, new GroovyScriptExecutor(tool.getCode(), tool.getInitParams()));
            }
        }
        return tools;
    }
}
