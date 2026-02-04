package com.tarzan.maxkb4j.module.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.GroovyScriptExecutor;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.tool.consts.ToolConstants;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
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

/**
 * 工具服务工具类，用于创建和管理工具规范和执行器
 */
@RequiredArgsConstructor
@Service
public class ToolUtilService {

    private static final String MCP_API_PATH = "/chat/api/mcp";
    private static final String MCP_TYPE = "streamable_http";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final String LOCALHOST = "http://127.0.0.1";

    private final ToolService toolService;
    private final ApplicationService applicationService;
    private final ApplicationApiKeyService apiKeyService;

    @Value("${server.port}")
    private int serverPort;

    public Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds, List<String> applicationIds) throws ApiException {
        // 参数验证
        if (CollectionUtils.isEmpty(toolIds) && CollectionUtils.isEmpty(applicationIds)) {
            return new HashMap<>();
        }
        
        Map<ToolSpecification, ToolExecutor> tools = getToolMap(toolIds);
        
        if (!CollectionUtils.isEmpty(applicationIds)) {
            JSONObject mcpServers = new JSONObject();
            LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                    .select(ApplicationEntity::getId, ApplicationEntity::getName)
                    .in(ApplicationEntity::getId, applicationIds);
            
            List<ApplicationEntity> applications = applicationService.list(wrapper);
            if (applications.isEmpty()) {
                throw new ApiException("No valid applications found for the provided application IDs");
            }
            
            for (ApplicationEntity app : applications) {
                JSONObject mcpConfig = getAppMcpConfig(app);
                mcpServers.put(app.getName(), mcpConfig);
            }
            tools.putAll(McpToolUtil.getToolMap(mcpServers));
        }
        return tools;
    }

    private JSONObject getAppMcpConfig(ApplicationEntity app) throws ApiException{
        ApplicationApiKeyEntity apiKey = apiKeyService.lambdaQuery().select(ApplicationApiKeyEntity::getSecretKey).eq(ApplicationApiKeyEntity::getApplicationId, app.getId()).last("limit 1").one();
        if (apiKey == null) {
            throw new ApiException(String.format("Agent Key is required for agent tool 【%s】", app.getName()));
        }
        JSONObject mcpConfig = new JSONObject();
        // 构建URL更安全的方式
        String url = String.format("%s:%d%s", LOCALHOST, serverPort, MCP_API_PATH);
        mcpConfig.put("url", url);
        mcpConfig.put("type", MCP_TYPE);
        
        // 创建headers更清晰
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("%s%s", AUTH_HEADER_PREFIX, apiKey.getSecretKey()));
        mcpConfig.put("headers", headers);
        
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
            if (ToolConstants.ToolType.MCP.equals(tool.getToolType())) {
                JSONObject mcpServers = JSONObject.parseObject(tool.getCode());
                tools.putAll(McpToolUtil.getToolMap(mcpServers));
            } else {
                List<ToolInputField> params = tool.getInputFieldList();
                JsonObjectSchema.Builder parametersBuilder = JsonObjectSchema.builder();
                for (ToolInputField param : params) {
                    JsonSchemaElement schemaElement = switch (param.getType()) {
                        case "string" -> {
                            parametersBuilder.addStringProperty(param.getName());
                            yield null;
                        }
                        case "int" -> {
                            parametersBuilder.addIntegerProperty(param.getName());
                            yield null;
                        }
                        case "number" -> {
                            parametersBuilder.addNumberProperty(param.getName());
                            yield null;
                        }
                        case "boolean" -> {
                            parametersBuilder.addBooleanProperty(param.getName());
                            yield null;
                        }
                        case "array" -> JsonArraySchema.builder().build();
                        case "object" -> JsonObjectSchema.builder().build();
                        default -> null;
                    };
                    if (schemaElement != null) {
                        parametersBuilder.addProperty(param.getName(), schemaElement);
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
