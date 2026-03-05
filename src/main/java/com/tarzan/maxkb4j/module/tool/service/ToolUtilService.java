package com.tarzan.maxkb4j.module.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.tool.consts.ToolConstants;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.executor.GroovyScriptExecutor;
import com.tarzan.maxkb4j.module.tool.executor.HttpRequestExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 工具服务工具类，用于创建和管理工具规范和执行器
 */
@RequiredArgsConstructor
@Service
@Slf4j
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

    /**
     * 获取工具映射：支持普通工具 + 应用（MCP）工具
     */
    public Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds, List<String> applicationIds) throws ApiException {
        if (CollectionUtils.isEmpty(toolIds) && CollectionUtils.isEmpty(applicationIds)) {
            return Collections.emptyMap();
        }

        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        // 1. 加载普通工具
        if (!CollectionUtils.isEmpty(toolIds)) {
            tools.putAll(buildToolMapFromToolEntities(toolIds));
        }

        // 2. 加载应用（MCP）工具
        if (!CollectionUtils.isEmpty(applicationIds)) {
            JSONObject mcpServers = buildMcpServerConfig(applicationIds);
            tools.putAll(McpToolUtil.getToolMap(mcpServers));
        }

        return tools;
    }

    /**
     * 根据工具 ID 列表构建工具映射
     */
    private Map<ToolSpecification, ToolExecutor> buildToolMapFromToolEntities(List<String> toolIds) {
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery(ToolEntity.class)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true);

        List<ToolEntity> toolEntities = toolService.list(wrapper);
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        for (ToolEntity tool : toolEntities) {
            try {
                if (ToolConstants.ToolType.MCP.equals(tool.getToolType())) {
                    JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
                    tools.putAll(McpToolUtil.getToolMap(mcpConfig));
                } else {
                    ToolSpecification spec = buildToolSpecification(tool);
                    ToolExecutor executor = createToolExecutor(tool);
                    tools.put(spec, executor);
                }
            } catch (Exception e) {
                log.warn("Failed to process tool: {}, error: {}", tool.getId(), e.getMessage(), e);
                // 可选：跳过错误工具 or 抛出异常
            }
        }
        return tools;
    }

    /**
     * 构建 MCP 服务器配置（用于应用工具）
     */
    private JSONObject buildMcpServerConfig(List<String> applicationIds) throws ApiException {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName)
                .in(ApplicationEntity::getId, applicationIds);

        List<ApplicationEntity> applications = applicationService.list(wrapper);
        if (applications.isEmpty()) {
            throw new ApiException("No valid applications found for the provided application IDs");
        }

        JSONObject mcpServers = new JSONObject();
        for (ApplicationEntity app : applications) {
            mcpServers.put(app.getName(), buildAppMcpConfig(app));
        }
        return mcpServers;
    }

    /**
     * 为单个应用构建 MCP 配置
     */
    private JSONObject buildAppMcpConfig(ApplicationEntity app) throws ApiException {
        ApplicationApiKeyEntity apiKey = apiKeyService.lambdaQuery()
                .select(ApplicationApiKeyEntity::getSecretKey)
                .eq(ApplicationApiKeyEntity::getApplicationId, app.getId())
                .last("LIMIT 1")
                .one();

        if (apiKey == null || apiKey.getSecretKey() == null) {
            throw new ApiException(String.format("Agent Key is required for agent tool 【%s】", app.getName()));
        }

        String url = String.format("%s:%d%s", LOCALHOST, serverPort, MCP_API_PATH);
        Map<String, String> headers = Collections.singletonMap(
                "Authorization",
                AUTH_HEADER_PREFIX + apiKey.getSecretKey()
        );

        JSONObject config = new JSONObject();
        config.put("url", url);
        config.put("type", MCP_TYPE);
        config.put("headers", headers);
        return config;
    }

    /**
     * 构建 ToolSpecification（参数 schema）
     */
    private ToolSpecification buildToolSpecification(ToolEntity tool) {
        JsonObjectSchema.Builder parametersBuilder = JsonObjectSchema.builder();

        List<ToolInputField> params = Optional.ofNullable(tool.getInputFieldList()).orElse(Collections.emptyList());
        for (ToolInputField param : params) {
            String type = param.getType();
            String name = param.getName();

            switch (type) {
                case "string" -> parametersBuilder.addStringProperty(name);
                case "int" -> parametersBuilder.addIntegerProperty(name);
                case "number" -> parametersBuilder.addNumberProperty(name);
                case "boolean" -> parametersBuilder.addBooleanProperty(name);
                case "array" -> parametersBuilder.addProperty(name, JsonArraySchema.builder().build());
                case "object" -> parametersBuilder.addProperty(name, JsonObjectSchema.builder().build());
                default -> log.warn("Unsupported parameter type: {} for field: {}", type, name);
            }
        }

        return ToolSpecification.builder()
                .name(tool.getName())
                .description(tool.getDesc())
                .parameters(parametersBuilder.build())
                .build();
    }

    /**
     * 创建对应的工具执行器
     */
    private ToolExecutor createToolExecutor(ToolEntity tool) {
        if (ToolConstants.ToolType.HTTP.equals(tool.getToolType())) {
            return new HttpRequestExecutor(tool.getCode());
        } else {
            return new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
        }
    }
}