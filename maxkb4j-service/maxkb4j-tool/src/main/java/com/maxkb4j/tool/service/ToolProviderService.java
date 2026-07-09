package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.executor.AgentExecutor;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.mp.entity.ToolInputField;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.executor.GroovyScriptExecutor;
import com.maxkb4j.tool.executor.HttpRequestExecutor;
import com.maxkb4j.tool.util.McpToolUtil;
import com.maxkb4j.tool.util.SkillsToolUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * 工具服务工具类，用于创建和管理工具规范和执行器
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolProviderService implements IToolProviderService {

    private final IToolService toolService;
    private final IApplicationService applicationService;
    private final IOssService ossService;
    private final IApplicationChatService chatService;
    private final IModelProviderService modelFactory;

    @Override
    public List<AiServiceTool> getTools(String chatModelId, List<String> toolIds, List<String> applicationIds) throws ApiException {
        List<AiServiceTool> tools = new ArrayList<>();
        if (CollectionUtils.isEmpty(toolIds) && CollectionUtils.isEmpty(applicationIds)) {
            return tools;
        }
        // 1. 加载普通工具
        if (!CollectionUtils.isEmpty(toolIds)) {
            tools.addAll(buildToolsFromToolIds(chatModelId, toolIds));
        }
        // 2. 加载智能体应用工具
        if (!CollectionUtils.isEmpty(applicationIds)) {
            tools.addAll(buildToolsFromAppIds(applicationIds));
        }
        return tools;
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
    public ShellSkills getShellSkills(List<String> toolIds) throws ApiException {
        List<ToolEntity> toolSkills = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getCode, ToolEntity::getInitParams)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true)
                .eq(ToolEntity::getToolType, ToolConstants.ToolType.SKILL)
                .list();
        List<FileSystemSkill> fileSystemSkills = new ArrayList<>();
        for (ToolEntity skill : toolSkills) {
            FileSystemSkill fileSystemSkill = this.getFileSystemSkill(skill.getId(), skill.getCode());
            fileSystemSkills.add(fileSystemSkill);
        }
        return ShellSkills.from(fileSystemSkills);
    }


    public FileSystemSkill getFileSystemSkill(String toolId, String code) throws ApiException {
        Path skillFolder = SkillsToolUtil.getSkillFolder(toolId);
        if (!Files.exists(skillFolder)) {
            unzipSkill(code, toolId);
        }
        return FileSystemSkillLoader.loadSkill(skillFolder);
    }


    public AiServiceTool getSkillsTool(ChatModel chatModel, ToolEntity skill) throws ApiException {
        ShellSkills skills = this.getShellSkill(skill.getId(), skill.getCode());
        if (skills != null) {
            String availableSkills = skills.formatAvailableSkills();
            Document doc = Jsoup.parse(availableSkills);
            Elements skillsElements = doc.getElementsByTag("skill");
            for (Element skillElement : skillsElements) {
                String name = skillElement.getElementsByTag("name").text();
                String description = skillElement.getElementsByTag("description").text();
                ToolSpecification spec = ToolSpecification.builder()
                        .name("tool_" + skill.getId())
                        .description("**" + name + "**" + ":" + description)
                        .parameters(JsonObjectSchema.builder().addStringProperty("question", "User's input question").required("question").build())
                        .build();
                ToolExecutor executor = (toolExecutionRequest, memoryId) -> {
                    JSONObject initParams = skill.getInitParams();
                    if (initParams != null && !initParams.isEmpty()) {
                        for (String key : initParams.keySet()) {
                            System.setProperty(key, initParams.getString(key));
                        }
                    }
                    Assistant assistant = AiServices.builder(Assistant.class)
                            .chatModel(chatModel)
                            .toolProvider(skills.toolProvider())
                            .systemMessage("You have access to the following skills:\n" + availableSkills + "\nWhen the user's request relates to one of these skills, read its SKILL.md before proceeding.")
                            .build();
                    JSONObject arguments = JSONObject.parseObject(toolExecutionRequest.arguments());
                    Result<String> result = assistant.chat(arguments.getString("question"));
                    if (initParams != null && !initParams.isEmpty()) {
                        for (String key : initParams.keySet()) {
                            System.clearProperty(key);
                        }
                    }
                    return result.content();
                };
                return AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build();
            }
        }
        return null;
    }


    private void unzipSkill(String fileId, String toolId) throws ApiException {
        if (!StringUtils.isEmpty(toolId) && !StringUtils.isEmpty(fileId)) {
            try (InputStream is = ossService.getStream(fileId)) {
                SkillsToolUtil.unzipSkill(is, toolId);
            } catch (IOException e) {
                throw new ApiException("tool.skill.file.extract.failed");
            }
        }
    }

    /**
     * 根据工具 ID 列表构建工具映射
     */
    private List<AiServiceTool> buildToolsFromToolIds(String chatModelId, List<String> toolIds) {
        List<ToolEntity> tools = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc, ToolEntity::getCode, ToolEntity::getCode, ToolEntity::getInitParams, ToolEntity::getInputFieldList, ToolEntity::getToolType)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true)
                .list();
        List<AiServiceTool> aiServiceTools = new ArrayList<>();
        if (tools.isEmpty()) {
            return aiServiceTools;
        }
        ChatModel chatModel = null;
        if (StringUtils.isNotBlank(chatModelId)){
             chatModel = modelFactory.buildChatModel(chatModelId);
        }
        for (ToolEntity tool : tools) {
            if (ToolConstants.ToolType.MCP.equals(tool.getToolType())) {
                JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
                aiServiceTools.addAll(McpToolUtil.getTools(mcpConfig));
            } else if (ToolConstants.ToolType.HTTP.equals(tool.getToolType())) {
                ToolSpecification spec = buildToolSpecification(tool);
                ToolExecutor executor = new HttpRequestExecutor(tool.getCode());
                aiServiceTools.add(AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build());
            } else if (ToolConstants.ToolType.SKILL.equals(tool.getToolType()) && chatModel!=null) {
                AiServiceTool aiServiceTool = this.getSkillsTool(chatModel, tool);
                if (aiServiceTool != null) {
                    aiServiceTools.add(aiServiceTool);
                }
            } else if (ToolConstants.ToolType.CUSTOM.equals(tool.getToolType())) {
                ToolSpecification spec = buildToolSpecification(tool);
                ToolExecutor executor = new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
                aiServiceTools.add(AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build());
            }
        }
        return aiServiceTools;
    }

    /**
     * 根据工具 ID 列表构建工具映射
     */

    private List<AiServiceTool> buildToolsFromAppIds(List<String> applicationIds) throws ApiException {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc)
                .in(ApplicationEntity::getId, applicationIds);
        List<ApplicationEntity> applications = applicationService.list(wrapper);
        List<AiServiceTool> tools = new ArrayList<>();
        if (applications.isEmpty()) {
            return tools;
        }
        for (ApplicationEntity app : applications) {
            ToolSpecification spec = buildToolSpecification(app);
            ToolExecutor executor = new AgentExecutor(app.getId(), chatService);
            tools.add(AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build());
        }
        return tools;
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