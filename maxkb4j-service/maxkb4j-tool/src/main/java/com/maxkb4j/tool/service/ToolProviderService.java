package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.executor.AgentExecutor;
import com.maxkb4j.application.executor.GroovyScriptExecutor;
import com.maxkb4j.application.executor.HttpRequestExecutor;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.mp.entity.ToolInputField;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.core.util.MessageUtils;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.util.McpToolUtil;
import com.maxkb4j.tool.util.SkillsToolUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


/**
 * 工具服务工具类，用于创建和管理工具规范和执行器
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolProviderService implements IToolProviderService {

    private final IToolService toolService;
    private final IApplicationService applicationService;
    private final IOssService mongoFileService;
    private final IApplicationChatService chatService;
    private final IModelProviderService modelFactory;

    /**
     * 获取工具映射：支持普通工具 + 应用（MCP）工具
     */
    @Override
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


    @Override
    public ShellSkills getSkills(List<String> toolIds) throws ApiException {
        List<FileSystemSkill> list = FileSystemSkillLoader.loadSkills(SkillsToolUtil.getSkillsFolder());
        if (list.isEmpty()) {
            return null;
        }
        return ShellSkills.from(list);
    }

    public ShellSkills getShellSkill(String toolId, String code) throws ApiException {
        Path skillFolder = SkillsToolUtil.getSkillFolder(toolId);
        if (!Files.exists(skillFolder)) {
            unzipSkill(code, toolId);
        }
        FileSystemSkill fileSystemSkill = FileSystemSkillLoader.loadSkill(skillFolder);
        if (fileSystemSkill == null) {
            return null;
        }
        return ShellSkills.from(fileSystemSkill);
    }


    @Override
    public ToolProvider getSkillsProvider(String modelId, List<String> toolIds) throws ApiException {
        Map<ToolSpecification, ToolExecutor> toolMap = getSkillsToolMap(modelId, toolIds);
        return (request) -> ToolProviderResult.builder().addAll(toolMap).build();
    }

    public Map<ToolSpecification, ToolExecutor> getSkillsToolMap(String modelId, List<String> toolIds) throws ApiException {
        Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();
        if (CollectionUtils.isEmpty(toolIds)){
            return toolMap;
        }
        ChatModel chatModel = modelFactory.buildChatModel(modelId);
        List<ToolEntity> toolSkills = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getCode)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true)
                .eq(ToolEntity::getToolType, ToolConstants.ToolType.SKILL)
                .list();
        for (ToolEntity skill : toolSkills) {
            ShellSkills skills = this.getShellSkill(skill.getId(), skill.getCode());
            if (skills != null) {
                String availableSkills = skills.formatAvailableSkills();
                Document doc = Jsoup.parse(availableSkills);
                Elements skillsElements = doc.getElementsByTag("skill");
                for (Element skillElement : skillsElements) {
                    String name = skillElement.getElementsByTag("name").text();
                    String description = skillElement.getElementsByTag("description").text();
                    ToolSpecification spec = ToolSpecification.builder()
                            .name(name)
                            .description(description)
                            .parameters(JsonObjectSchema.builder().addStringProperty("question", "User's input question").required("question").build())
                            .build();
                    ToolExecutor executor = (toolExecutionRequest, memoryId) -> {
                        Assistant assistant = AssistantServices.builder(Assistant.class).chatModel(chatModel).toolProvider(skills.toolProvider()).build();
                        JSONObject arguments = JSONObject.parseObject(toolExecutionRequest.arguments());
                        return assistant.chat(arguments.getString("question")).content();
                    };
                    toolMap.put(spec, executor);
                }
            }
        }
        return toolMap;
    }


    private void unzipSkill(String fileId, String toolId) throws ApiException {
        if (!StringUtils.isEmpty(toolId) && !StringUtils.isEmpty(fileId)) {
            try (InputStream is = mongoFileService.getStream(fileId)) {
                SkillsToolUtil.unzipSkill(is, toolId);
            } catch (IOException e) {
                throw new ApiException("Failed to extract the skill file.");
            }
        }
    }

    /**
     * 根据工具 ID 列表构建工具映射
     */
    private Map<ToolSpecification, ToolExecutor> buildToolMapFromToolIds(List<String> toolIds) {
        List<ToolEntity> tools = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc, ToolEntity::getCode, ToolEntity::getCode, ToolEntity::getInitParams, ToolEntity::getInputFieldList, ToolEntity::getToolType)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true)
                .in(ToolEntity::getToolType, ToolConstants.ToolType.MCP, ToolConstants.ToolType.CUSTOM, ToolConstants.ToolType.HTTP)
                .list();
        Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();
        if (tools.isEmpty()) {
            return toolMap;
        }
        for (ToolEntity tool : tools) {
            if (ToolConstants.ToolType.MCP.equals(tool.getToolType())) {
                JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
                toolMap.putAll(McpToolUtil.getToolMap(mcpConfig));
            } else if (ToolConstants.ToolType.HTTP.equals(tool.getToolType())) {
                ToolSpecification spec = buildToolSpecification(tool);
                ToolExecutor executor = new HttpRequestExecutor(tool.getCode());
                toolMap.put(spec, executor);
            } else if (ToolConstants.ToolType.CUSTOM.equals(tool.getToolType())) {
                ToolSpecification spec = buildToolSpecification(tool);
                ToolExecutor executor = new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
                toolMap.put(spec, executor);
            }
        }
        return toolMap;
    }


    /**
     * 构建 MCP 服务器配置（用于应用工具）
     */
    private Map<ToolSpecification, ToolExecutor> buildToolMapFromAppIds(List<String> applicationIds) throws ApiException {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc)
                .in(ApplicationEntity::getId, applicationIds);
        List<ApplicationEntity> applications = applicationService.list(wrapper);
        Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();
        if (applications.isEmpty()) {
            return toolMap;
        }
        for (ApplicationEntity app : applications) {
            ToolSpecification spec = buildToolSpecification(app);
            ToolExecutor executor = new AgentExecutor(app.getId(), chatService);
            toolMap.put(spec, executor);
        }
        return toolMap;
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

    @Override
    public String format(ToolExecution toolExecute) {
        String name = toolExecute.request().name();
        String[] split = name.split("_");
        if (split.length < 2) {
            return MessageUtils.buildToolCallRender("", name, "", toolExecute.request().arguments(), toolExecute.result());
        }
        String type = split[0];
        String id = split[1];
        if ("tool".equals(type)) {
            ToolEntity tool = toolService.lambdaQuery().select(ToolEntity::getIcon, ToolEntity::getName).eq(ToolEntity::getId, id).one();
            if (tool == null) {
                return MessageUtils.buildToolCallRender("", name, "", toolExecute.request().arguments(), toolExecute.result());
            }
            return MessageUtils.buildToolCallRender(tool.getIcon(), tool.getName(), tool.getToolType(), toolExecute.request().arguments(), toolExecute.result());
        } else {
            ApplicationEntity app = applicationService.lambdaQuery().select(ApplicationEntity::getIcon, ApplicationEntity::getName).eq(ApplicationEntity::getId, id).one();
            if (app == null) {
                return MessageUtils.buildToolCallRender("", name, "", toolExecute.request().arguments(), toolExecute.result());
            }
            return MessageUtils.buildToolCallRender(app.getIcon(), app.getName(), "", toolExecute.request().arguments(), toolExecute.result());
        }

    }

}