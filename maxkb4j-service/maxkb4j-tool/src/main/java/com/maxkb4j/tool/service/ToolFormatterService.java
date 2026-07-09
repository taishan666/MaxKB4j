package com.maxkb4j.tool.service;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.core.util.MessageUtils;
import com.maxkb4j.tool.entity.ToolEntity;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 工具调用格式化服务实现，将工具执行过程渲染为前端展示文本
 */
@Service
@RequiredArgsConstructor
public class ToolFormatterService implements IToolFormatterService {

    private final IToolService toolService;
    private final IApplicationService applicationService;

    @Override
    public String format(ToolExecution toolExecute) {
        int status = toolExecute.hasFailed() ? 400 : 200;
        return format(toolExecute.request(), status, toolExecute.result());
    }

    @Override
    public String format(BeforeToolExecution toolExecute) {
        return format(toolExecute.request(), 100, "");
    }

    public String format(ToolExecutionRequest request, Integer status, String resultText) {
        String name = request.name();
        String[] split = name.split("_");
        if (split.length < 2) {
            return MessageUtils.buildToolCallRender(request.id(), status, "", name, "", request.arguments(), resultText);
        }
        String type = split[0];
        String id = split[1];
        if ("tool".equals(type)) {
            ToolEntity tool = toolService.lambdaQuery().select(ToolEntity::getIcon, ToolEntity::getToolType, ToolEntity::getName).eq(ToolEntity::getId, id).one();
            if (tool == null) {
                return MessageUtils.buildToolCallRender(request.id(), status, "", name, "", request.arguments(), resultText);
            }
            return MessageUtils.buildToolCallRender(request.id(), status, tool.getIcon(), tool.getName(), tool.getToolType(), request.arguments(), resultText);
        } else {
            ApplicationEntity app = applicationService.lambdaQuery().select(ApplicationEntity::getIcon, ApplicationEntity::getName).eq(ApplicationEntity::getId, id).one();
            if (app == null) {
                return MessageUtils.buildToolCallRender(request.id(), status, "", name, "", request.arguments(), resultText);
            }
            return MessageUtils.buildToolCallRender(request.id(), status, app.getIcon(), app.getName(), "", request.arguments(), resultText);
        }
    }
}
