package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.util.SkillsToolUtil;
import com.maxkb4j.tool.util.ToolNaming;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.shell.RunShellCommandToolConfig;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skill 工具服务，负责 Skill 类型工具的文件管理、加载和执行
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillToolService {

    private final IToolService toolService;
    private final IOssService ossService;

    /**
     * 获取 ShellSkills（多工具合并）
     */
    public ShellSkills getShellSkillsByToolIds(List<String> toolIds) throws ApiException {
        List<ToolEntity> toolSkills = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getCode, ToolEntity::getInitParams)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true)
                .eq(ToolEntity::getToolType, ToolConstants.ToolType.SKILL)
                .list();
        return getShellSkills(toolSkills);
    }

    public ShellSkills getShellSkills(List<ToolEntity> toolSkills) throws ApiException {
        if (toolSkills.isEmpty()) {
            return null;
        }
        List<FileSystemSkill> fileSystemSkills = new ArrayList<>();
        for (ToolEntity skill : toolSkills) {
            fileSystemSkills.add(getFileSystemSkill(skill.getId(), skill.getCode()));
        }
        RunShellCommandToolConfig config = RunShellCommandToolConfig.builder().build();
        return ShellSkills.builder()
                .skills(fileSystemSkills)
                .runShellCommandToolConfig(config)
                .build();
    }

    public List<ToolProvider> getShellSkillsToolProviders(List<ToolEntity> toolSkills) throws ApiException {
        if (toolSkills.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolProvider> toolProviders = new ArrayList<>();
        for (ToolEntity skill : toolSkills) {
            FileSystemSkill fileSystemSkill = getFileSystemSkill(skill.getId(), skill.getCode());
            RunShellCommandToolConfig config = RunShellCommandToolConfig.builder()
                    .name(ToolNaming.buildToolName(skill.getId()))
                    .build();
            ShellSkills skills = ShellSkills.builder()
                    .skills(fileSystemSkill)
                    .runShellCommandToolConfig(config)
                    .build();
            toolProviders.add(skills.toolProvider());
        }
        return toolProviders;
    }

    /**
     * 构建单个 Skill 工具的 AiServiceTool 列表（基于 chatMemoryId 模式）
     */
    public List<AiServiceTool> getSkillsTools(String userMessage, ToolEntity tool) throws ApiException {
        FileSystemSkill fileSystemSkill = getFileSystemSkill(tool.getId(), tool.getCode());
        RunShellCommandToolConfig config = RunShellCommandToolConfig.builder()
                .name(ToolNaming.buildToolName(tool.getId()))
                .build();
        ShellSkills skills = ShellSkills.builder()
                .skills(fileSystemSkill)
                .runShellCommandToolConfig(config)
                .build();
        ToolProviderRequest toolProviderRequest = new ToolProviderRequest("default", UserMessage.from(userMessage));
        ToolProviderResult toolProviderResult = skills.toolProvider().provideTools(toolProviderRequest);
        return toolProviderResult.aiServiceTools();
    }


    private FileSystemSkill getFileSystemSkill(String toolId, String code) throws ApiException {
        Path skillFolder = SkillsToolUtil.getSkillFolder(toolId);
        if (!Files.exists(skillFolder)) {
            unzipSkill(code, toolId);
        }
        return FileSystemSkillLoader.loadSkill(skillFolder);
    }

    private void unzipSkill(String fileId, String toolId) throws ApiException {
        if (StringUtils.isEmpty(toolId) || StringUtils.isEmpty(fileId)) {
            return;
        }
        try (InputStream is = ossService.getStream(fileId)) {
            SkillsToolUtil.unzipSkill(is, toolId);
        } catch (IOException e) {
            throw new ApiException("tool.skill.file.extract.failed");
        }
    }

    private void setInitParams(JSONObject initParams) {
        if (initParams != null && !initParams.isEmpty()) {
            for (String key : initParams.keySet()) {
                System.setProperty(key, initParams.getString(key));
            }
        }
    }

    private void clearInitParams(JSONObject initParams) {
        if (initParams != null && !initParams.isEmpty()) {
            for (String key : initParams.keySet()) {
                System.clearProperty(key);
            }
        }
    }
}
