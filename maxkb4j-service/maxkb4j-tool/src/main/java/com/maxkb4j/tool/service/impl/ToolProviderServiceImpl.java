package com.maxkb4j.tool.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.executor.GroovyScriptExecutor;
import com.maxkb4j.tool.executor.HttpRequestExecutor;
import com.maxkb4j.tool.service.IAgentToolService;
import com.maxkb4j.tool.service.IToolProviderService;
import com.maxkb4j.tool.service.SkillToolService;
import com.maxkb4j.tool.service.ToolSpecificationBuilder;
import com.maxkb4j.tool.util.McpToolUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
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
 * 工具提供者服务，用于创建和管理工具规范和执行器。
 * <p>
 * 作为编排器，将不同工具类型（MCP/HTTP/SKILL/CUSTOM）的分发委托给专门的子服务：
 * <ul>
 *   <li>{@link SkillToolService} — Skill 工具的加载与执行</li>
 *   <li>{@link IAgentToolService} — 智能体应用工具构建</li>
 *   <li>{@link ToolSpecificationBuilder} — 工具规范（参数 schema）构建</li>
 * </ul>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolProviderServiceImpl implements IToolProviderService {

    private final ToolServiceImpl toolService;
    private final SkillToolService skillToolService;
    private final IAgentToolService agentToolService;
    private final ToolSpecificationBuilder toolSpecificationBuilder;


    @Override
    public List<AiServiceTool> getTools(String userMessage, List<String> toolIds, List<String> applicationIds) throws ApiException {
        List<AiServiceTool> tools = new ArrayList<>();
        if (CollectionUtils.isEmpty(toolIds) && CollectionUtils.isEmpty(applicationIds)) {
            return tools;
        }
        if (!CollectionUtils.isEmpty(toolIds)&& StringUtils.isNotBlank(userMessage)) {
            tools.addAll(buildAiServiceTools(toolIds, tool -> skillToolService.getSkillsTools(userMessage, tool)));
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
            ToolProvider toolProvider =agentToolService.buildToolProvider(applicationIds);
            if (toolProvider != null){
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

    // ===== 私有方法：查询 =====

    /**
     * 查询激活状态的工具，可选按工具类型过滤
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

    // ===== 私有方法：构建 AiServiceTool =====

    /**
     * 构建工具列表为 AiServiceTool，通过 skillResolver 处理 SKILL 类型
     */
    private List<AiServiceTool> buildAiServiceTools(List<String> toolIds, SkillToolResolver skillResolver) {
        List<ToolEntity> tools = queryActiveTools(toolIds);
        List<AiServiceTool> aiServiceTools = new ArrayList<>();
        for (ToolEntity tool : tools) {
            String toolType = tool.getToolType();
            if (ToolConstants.ToolType.MCP.equals(toolType)) {
                aiServiceTools.addAll(buildMcpTools(tool));
            } else if (ToolConstants.ToolType.HTTP.equals(toolType)) {
                aiServiceTools.add(buildHttpTool(tool));
            } else if (ToolConstants.ToolType.SKILL.equals(toolType)&&skillResolver!=null) {
                List<AiServiceTool> skillsTools = skillResolver.resolve(tool);
                if (CollectionUtils.isNotEmpty(skillsTools)) {
                    aiServiceTools.addAll(skillsTools);
                }
            } else if (ToolConstants.ToolType.CUSTOM.equals(toolType)) {
                aiServiceTools.add(buildCustomTool(tool));
            }
        }
        return aiServiceTools;
    }

    private List<AiServiceTool> buildMcpTools(ToolEntity tool) {
        JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
        return McpToolUtil.getTools(mcpConfig);
    }

    private AiServiceTool buildHttpTool(ToolEntity tool) {
        ToolSpecification spec = toolSpecificationBuilder.build(tool);
        ToolExecutor executor = new HttpRequestExecutor(tool.getCode());
        return AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build();
    }

    private AiServiceTool buildCustomTool(ToolEntity tool) {
        ToolSpecification spec = toolSpecificationBuilder.build(tool);
        ToolExecutor executor = new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
        return AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build();
    }

    // ===== 私有方法：构建 ToolProvider =====

    /**
     * 构建工具列表为 ToolProvider（仅 MCP/HTTP/CUSTOM）
     */
    private List<ToolProvider> buildToolProviders(List<String> toolIds) {
        List<ToolEntity> tools = queryActiveTools(toolIds,
                ToolConstants.ToolType.MCP,ToolConstants.ToolType.SKILL,ToolConstants.ToolType.HTTP, ToolConstants.ToolType.CUSTOM);
        List<ToolProvider> toolProviders = new ArrayList<>();
        if (tools.isEmpty()) {
            return toolProviders;
        }
        Map<String, List<ToolEntity>> toolMap = tools.stream()
                .collect(Collectors.groupingBy(ToolEntity::getToolType));
        toolMap.forEach((toolType, toolList) -> {
            if (ToolConstants.ToolType.MCP.equals(toolType)) {
                for (ToolEntity tool : toolList) {
                    JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
                    McpToolProvider mcpToolProvider = McpToolUtil.getMcpToolProvider(mcpConfig);
                    if (mcpToolProvider != null) {
                        toolProviders.add(mcpToolProvider);
                    }
                }
            }else if (ToolConstants.ToolType.SKILL.equals(toolType)) {
                toolProviders.addAll(skillToolService.getShellSkillsToolProviders(toolList));
            }else if (ToolConstants.ToolType.HTTP.equals(toolType)) {
                toolProviders.add(wrapAsToolProvider(toolList, tool -> new HttpRequestExecutor(tool.getCode())));
            } else if (ToolConstants.ToolType.CUSTOM.equals(toolType)) {
                toolProviders.add(wrapAsToolProvider(toolList, tool -> new GroovyScriptExecutor(tool.getCode(), tool.getInitParams())));
            }
        });
        return toolProviders;
    }

    /**
     * 将多个工具包装为单个 ToolProvider
     */
    private ToolProvider wrapAsToolProvider(List<ToolEntity> toolList, ToolExecutorFactory executorFactory) {
        List<AiServiceTool> aiServiceTools = new ArrayList<>();
        for (ToolEntity tool : toolList) {
            ToolSpecification spec = toolSpecificationBuilder.build(tool);
            ToolExecutor executor = executorFactory.create(tool);
            aiServiceTools.add(AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build());
        }
        return toolProviderRequest -> ToolProviderResult.builder().addAll(aiServiceTools).build();
    }

    // ===== 函数式接口 =====

    @FunctionalInterface
    private interface SkillToolResolver {
        List<AiServiceTool> resolve(ToolEntity tool);
    }

    @FunctionalInterface
    private interface ToolExecutorFactory {
        ToolExecutor create(ToolEntity tool);
    }
}
