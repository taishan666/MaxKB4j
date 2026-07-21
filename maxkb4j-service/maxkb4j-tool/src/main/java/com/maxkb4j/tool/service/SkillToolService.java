package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.handler.ToolSkillHandler;
import com.maxkb4j.tool.service.impl.ToolProviderServiceImpl;
import com.maxkb4j.tool.util.ToolNaming;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.shell.RunShellCommandToolConfig;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Skill 工具服务，负责 Skill 类型工具的加载与 ShellSkills / ToolProvider / AiServiceTool 构建。
 *
 * <p>文件管理（解压、懒加载）委托给 {@link ToolSkillHandler}，数据库查询由
 * {@link ToolProviderServiceImpl} 编排，本服务仅关注 Skill 的加载与 langchain4j 工具对象的构建。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillToolService {

    private final ToolSkillHandler toolSkillHandler;

    /**
     * 构建合并多个 Skill 的 ShellSkills（使用默认 RunShellCommandToolConfig）。
     *
     * @param toolSkills 已查询好的工具实体列表
     * @return 合并后的 ShellSkills；列表为空时返回 null
     */
    public ShellSkills getShellSkills(List<ToolEntity> toolSkills) throws ApiException {
        if (toolSkills.isEmpty()) {
            return null;
        }
        List<FileSystemSkill> fileSystemSkills = new ArrayList<>();
        for (ToolEntity skill : toolSkills) {
            fileSystemSkills.add(loadFileSystemSkill(skill));
        }
        return ShellSkills.builder()
                .skills(fileSystemSkills)
                .runShellCommandToolConfig(RunShellCommandToolConfig.builder().build())
                .build();
    }

    /**
     * 为每个 Skill 构建独立的 ToolProvider（各自带 tool_&lt;id&gt; 命名）。
     *
     * @param toolSkills 已查询好的工具实体列表
     * @return ToolProvider 列表；输入为空时返回空列表
     */
    public List<ToolProvider> getShellSkillsToolProviders(List<ToolEntity> toolSkills) throws ApiException {
        if (toolSkills.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolProvider> toolProviders = new ArrayList<>();
        for (ToolEntity skill : toolSkills) {
            FileSystemSkill fileSystemSkill = loadFileSystemSkill(skill);
            toolProviders.add(buildNamedShellSkills(fileSystemSkill, skill.getId()).toolProvider());
        }
        return toolProviders;
    }

    /**
     * 构建单个 Skill 工具的 AiServiceTool 列表（基于 chatMemoryId 模式）。
     *
     * @param userMessage 用户消息
     * @param tool        工具实体
     * @return AiServiceTool 列表
     */
    public List<AiServiceTool> getSkillsTools(String userMessage, ToolEntity tool) throws ApiException {
        FileSystemSkill fileSystemSkill = loadFileSystemSkill(tool);
        ShellSkills shellSkills = buildNamedShellSkills(fileSystemSkill, tool.getId());
        ToolProviderRequest request = new ToolProviderRequest("default", UserMessage.from(userMessage));
        ToolProviderResult result = shellSkills.toolProvider().provideTools(request);
        return result.aiServiceTools();
    }

    // ===== 私有方法 =====

    /**
     * 加载 FileSystemSkill，若本地目录不存在则触发解压（委托 ToolSkillHandler 懒加载）。
     */
    private FileSystemSkill loadFileSystemSkill(ToolEntity tool) throws ApiException {
        return toolSkillHandler.loadSkill(tool.getId(), tool.getCode());
    }

    /**
     * 构建带 tool_&lt;id&gt; 命名的 ShellSkills（单 Skill 场景）。
     */
    private ShellSkills buildNamedShellSkills(FileSystemSkill fileSystemSkill, String toolId) {
        RunShellCommandToolConfig config = RunShellCommandToolConfig.builder()
                .name(ToolNaming.buildToolName(toolId))
                .build();
        return ShellSkills.builder()
                .skills(fileSystemSkill)
                .runShellCommandToolConfig(config)
                .build();
    }
}
