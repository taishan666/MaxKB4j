package com.maxkb4j.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.executor.AgentExecutor;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用工具服务，负责将智能体应用构建为可执行的工具
 */
@Service
@RequiredArgsConstructor
public class ApplicationToolService {

    private final IApplicationService applicationService;
    private final IApplicationChatService chatService;
    private final ToolSpecificationBuilder toolSpecificationBuilder;

    /**
     * 根据应用 ID 列表构建 AiServiceTool 列表
     */
    public List<AiServiceTool> buildTools(List<String> applicationIds) {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc)
                .in(ApplicationEntity::getId, applicationIds);
        List<ApplicationEntity> applications = applicationService.list(wrapper);
        List<AiServiceTool> tools = new ArrayList<>();
        if (applications.isEmpty()) {
            return tools;
        }
        for (ApplicationEntity app : applications) {
            ToolSpecification spec = toolSpecificationBuilder.build(app);
            ToolExecutor executor = new AgentExecutor(app.getId(), chatService);
            tools.add(AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build());
        }
        return tools;
    }

    public ToolProvider buildToolProvider(List<String> applicationIds) {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc)
                .in(ApplicationEntity::getId, applicationIds);
        List<ApplicationEntity> applications = applicationService.list(wrapper);
        if (applications.isEmpty()) {
            return null;
        }
        List<AiServiceTool> aiServiceTools = new ArrayList<>();
        for (ApplicationEntity app : applications) {
            ToolSpecification spec = toolSpecificationBuilder.build(app);
            ToolExecutor executor = new AgentExecutor(app.getId(), chatService);
            aiServiceTools.add(AiServiceTool.builder().toolSpecification(spec).toolExecutor(executor).build());
        }
        return toolProviderRequest -> ToolProviderResult.builder().addAll(aiServiceTools).build();
    }
}
