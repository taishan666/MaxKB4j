package com.tarzan.maxkb4j.module.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.module.tool.consts.ToolConstants;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.executor.AgentExecutor;
import com.tarzan.maxkb4j.module.tool.executor.GroovyScriptExecutor;
import com.tarzan.maxkb4j.module.tool.executor.HttpRequestExecutor;
import com.tarzan.maxkb4j.module.tool.util.McpToolUtil;
import com.tarzan.maxkb4j.module.tool.util.SkillsToolUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * 工具服务工具类，用于创建和管理工具规范和执行器
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolProviderService {

    private static final String MCP_API_PATH = "/chat/api/mcp";
    private static final String MCP_TYPE = "streamable_http";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final String LOCALHOST = "http://127.0.0.1";

    private final ToolService toolService;
    private final ApplicationService applicationService;
    private final ApplicationApiKeyService apiKeyService;
    private final MongoFileService mongoFileService;
    private final ApplicationChatService chatService;

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
            tools.putAll(buildToolMapFromToolIds(toolIds));
        }
        // 2. 加载智能体应用工具
        if (!CollectionUtils.isEmpty(applicationIds)) {
            tools.putAll(buildToolMapFromAppIds(applicationIds));
        }
        return tools;
    }

    public ToolProvider getSkillsToolProvider(String applicationId, String nodeId, List<String> toolIds) throws ApiException {
        String appSkillPath = "app/" + applicationId + "/" + nodeId + "/skills/";
        return buildToolProvider(appSkillPath, toolIds);
    }

    public ToolProvider getSkillsToolProvider(String applicationId, List<String> toolIds) throws ApiException {
        String appSkillPath = "app/" + applicationId + "/skills/";
        return buildToolProvider(appSkillPath, toolIds);
    }

    // 公共逻辑
    private ToolProvider buildToolProvider(String appSkillPath, List<String> toolIds) throws ApiException {
        Path appSkillFolderPath = Paths.get(appSkillPath);
        Map<String, String> manifest = SkillsToolUtil.readManifest(appSkillPath);
        List<String> newToolIds = SkillsToolUtil.getAddShills(appSkillPath, toolIds, manifest);
        unzipSkills(appSkillFolderPath, newToolIds, manifest);
        SkillsToolUtil.updateManifest(appSkillPath, manifest);
        Skills skills = Skills.from(FileSystemSkillLoader.loadSkills(appSkillFolderPath));
        return skills.toolProvider();
    }



    private  void unzipSkills(Path appSkillFolderPath, List<String> newToolIds,Map<String, String> manifestToUpdate) {
        try {
            Files.createDirectories(appSkillFolderPath); // 自动创建多级目录
        } catch (IOException e) {
            throw new ApiException("Failed to create skill directory: " + e.getMessage());
        }
        List<ToolEntity> skillTools = toolService.lambdaQuery()
                .select(ToolEntity::getCode)
                .in(ToolEntity::getId, newToolIds)
                .eq(ToolEntity::getIsActive, true)
                .eq(ToolEntity::getToolType, ToolConstants.ToolType.SKILL)
                .list();
        for (ToolEntity skill : skillTools) {
            try (InputStream is = mongoFileService.getStream(skill.getCode())) {
                String folderName= SkillsToolUtil.unzipSkill(appSkillFolderPath,is);
                manifestToUpdate.put(skill.getId(), folderName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 根据工具 ID 列表构建工具映射
     */
    private Map<ToolSpecification, ToolExecutor> buildToolMapFromToolIds(List<String> toolIds) {
        List<ToolEntity> toolEntities = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc, ToolEntity::getCode, ToolEntity::getCode, ToolEntity::getInitParams, ToolEntity::getInputFieldList, ToolEntity::getToolType)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true)
                .in(ToolEntity::getToolType, ToolConstants.ToolType.MCP, ToolConstants.ToolType.CUSTOM, ToolConstants.ToolType.HTTP)
                .list();
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        for (ToolEntity tool : toolEntities) {
            try {
                if (ToolConstants.ToolType.MCP.equals(tool.getToolType())) {
                    JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
                    tools.putAll(McpToolUtil.getToolMap(mcpConfig));
                }
                if (ToolConstants.ToolType.HTTP.equals(tool.getToolType())) {
                    ToolSpecification spec = buildToolSpecification(tool);
                    ToolExecutor executor = new HttpRequestExecutor(tool.getCode());
                    tools.put(spec, executor);
                } else if (ToolConstants.ToolType.CUSTOM.equals(tool.getToolType())) {
                    ToolSpecification spec = buildToolSpecification(tool);
                    ToolExecutor executor = new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
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
    private Map<ToolSpecification, ToolExecutor> buildToolMapFromAppIds(List<String> applicationIds) throws ApiException {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc)
                .in(ApplicationEntity::getId, applicationIds);
        List<ApplicationEntity> applications = applicationService.list(wrapper);
        if (applications.isEmpty()) {
            throw new ApiException("No valid applications found for the provided application IDs");
        }
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        for (ApplicationEntity app : applications) {
            ToolSpecification spec = buildToolSpecification(app);
            ToolExecutor executor = new AgentExecutor(app.getId(), chatService);
            tools.put(spec, executor);
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
        List<String> required = new ArrayList<>();
        for (ToolInputField param : params) {
            String type = param.getType();
            String name = param.getName();
            boolean isRequired = param.getIsRequired();
            if (isRequired) {
                required.add(name);
            }
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
        if (!required.isEmpty()) {
            parametersBuilder.required(required);
        }
        return ToolSpecification.builder()
                .name("tool_" + tool.getId())
                .description("**" + tool.getName() + "**" + ":" + tool.getDesc())
                .parameters(parametersBuilder.build())
                .build();
    }


    private ToolSpecification buildToolSpecification(ApplicationEntity app) {
        JsonObjectSchema parameters = JsonObjectSchema.builder()
                .addStringProperty("message")
                .required("message")
                .build();
        return ToolSpecification.builder()
                .name("agent_" + app.getId())
                .description("**" + app.getName() + "**" + ":" + app.getDesc())
                .parameters(parameters)
                .build();
    }

}