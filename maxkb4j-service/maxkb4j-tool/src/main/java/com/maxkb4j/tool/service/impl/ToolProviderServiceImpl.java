package com.maxkb4j.tool.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.provider.AbsToolHandler;
import com.maxkb4j.tool.registry.ToolHandlerRegistry;
import com.maxkb4j.tool.service.IAgentToolService;
import com.maxkb4j.tool.service.IToolProviderService;
import com.maxkb4j.tool.service.SkillToolService;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool provider service: creates and manages tool specifications and executors.
 *
 * <p>Dispatch by tool type (MCP/HTTP/SKILL/CUSTOM) is delegated to
 * {@link AbsToolHandler} implementations discovered via {@link ToolHandlerRegistry};
 * adding a new tool type only requires a new {@code @ToolHandlerType} handler, with no
 * changes here (open/closed). This replaces the former if/else chains on toolType.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolProviderServiceImpl implements IToolProviderService {

    private final ToolServiceImpl toolService;
    private final SkillToolService skillToolService;
    private final IAgentToolService agentToolService;
    private final ToolHandlerRegistry toolHandlerRegistry;


    @Override
    public List<AiServiceTool> getTools(String userMessage, List<String> toolIds, List<String> applicationIds) throws ApiException {
        List<AiServiceTool> tools = new ArrayList<>();
        if (CollectionUtils.isEmpty(toolIds) && CollectionUtils.isEmpty(applicationIds)) {
            return tools;
        }
        if (!CollectionUtils.isEmpty(toolIds) && StringUtils.isNotBlank(userMessage)) {
            tools.addAll(buildAiServiceTools(toolIds, userMessage));
        }
        if (!CollectionUtils.isEmpty(applicationIds)) {
            tools.addAll(agentToolService.buildTools(applicationIds));
        }
        return tools;
    }

    @Override
    public List<ToolProvider> getToolProviders(List<String> toolIds, List<String> applicationIds) throws ApiException {
        List<ToolProvider> toolProviders = new ArrayList<>();
        if (CollectionUtils.isEmpty(toolIds) && CollectionUtils.isEmpty(applicationIds)) {
            return toolProviders;
        }
        if (!CollectionUtils.isEmpty(toolIds)) {
            toolProviders.addAll(buildToolProviders(toolIds));
        }
        if (!CollectionUtils.isEmpty(applicationIds)) {
            ToolProvider toolProvider = agentToolService.buildToolProvider(applicationIds);
            if (toolProvider != null) {
                toolProviders.add(toolProvider);
            }
        }
        return toolProviders;
    }

    @Override
    public ShellSkills getShellSkills(List<String> toolIds) throws ApiException {
        List<ToolEntity> tools = queryActiveTools(toolIds, ToolConstants.ToolType.SKILL);
        return skillToolService.getShellSkills(tools);
    }

    // ===== private: query =====

    /**
     * Query active tools, optionally filtered by tool type.
     */
    private List<ToolEntity> queryActiveTools(List<String> toolIds, String... toolTypes) {
        var query = toolService.lambdaQuery()
                .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc,
                        ToolEntity::getCode, ToolEntity::getInitParams,
                        ToolEntity::getInputFieldList, ToolEntity::getToolType)
                .in(ToolEntity::getId, toolIds)
                .eq(ToolEntity::getIsActive, true);
        if (toolTypes != null && toolTypes.length > 0) {
            query.in(ToolEntity::getToolType, (Object[]) toolTypes);
        }
        return query.list();
    }

    // ===== private: build AiServiceTool =====

    /**
     * Build AiServiceTools by dispatching each tool to its registered handler.
     */
    private List<AiServiceTool> buildAiServiceTools(List<String> toolIds, String userMessage) {
        List<ToolEntity> tools = queryActiveTools(toolIds);
        List<AiServiceTool> aiServiceTools = new ArrayList<>();
        for (ToolEntity tool : tools) {
            AbsToolHandler handler = toolHandlerRegistry.get(tool.getToolType());
            if (handler == null) {
                continue;
            }
            aiServiceTools.addAll(handler.buildAiServiceTools(tool, userMessage));
        }
        return aiServiceTools;
    }

    // ===== private: build ToolProvider =====

    /**
     * Build ToolProviders by dispatching each tool-type group to its registered handler.
     */
    private List<ToolProvider> buildToolProviders(List<String> toolIds) {
        List<ToolEntity> tools = queryActiveTools(toolIds,
                ToolConstants.ToolType.MCP, ToolConstants.ToolType.SKILL,
                ToolConstants.ToolType.HTTP, ToolConstants.ToolType.CUSTOM);
        List<ToolProvider> toolProviders = new ArrayList<>();
        if (tools.isEmpty()) {
            return toolProviders;
        }
        Map<String, List<ToolEntity>> toolMap = tools.stream()
                .collect(Collectors.groupingBy(ToolEntity::getToolType));
        toolMap.forEach((toolType, toolList) -> {
            AbsToolHandler handler = toolHandlerRegistry.get(toolType);
            if (handler != null) {
                toolProviders.addAll(handler.buildToolProviders(toolList));
            }
        });
        return toolProviders;
    }
}