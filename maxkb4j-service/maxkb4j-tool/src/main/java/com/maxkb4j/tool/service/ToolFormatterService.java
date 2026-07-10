package com.maxkb4j.tool.service;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.core.util.MessageUtils;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.util.ToolNaming;
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

    private String format(ToolExecutionRequest request, int status, String resultText) {
        ToolNaming.Ref ref = ToolNaming.parse(request.name());
        RenderMeta meta = ref == null ? null : resolveMeta(ref.type(), ref.id());
        if (meta == null) {
            meta = RenderMeta.fallback(request.name());
        }
        return MessageUtils.buildToolCallRender(
                request.id(), status, meta.icon(), meta.name(), meta.toolType(),
                request.arguments(), resultText);
    }

    /**
     * 按工具名称类型解析展示元数据；未命中返回 null 由调用方走 fallback
     */
    private RenderMeta resolveMeta(String type, String id) {
        return switch (type) {
            case ToolNaming.TOOL_TYPE -> resolveToolMeta(id);
            case ToolNaming.AGENT_TYPE -> resolveAgentMeta(id);
            default -> null;
        };
    }

    private RenderMeta resolveToolMeta(String id) {
        ToolEntity tool = toolService.lambdaQuery()
                .select(ToolEntity::getIcon, ToolEntity::getToolType, ToolEntity::getName)
                .eq(ToolEntity::getId, id)
                .one();
        return tool == null ? null : new RenderMeta(tool.getIcon(), tool.getName(), tool.getToolType());
    }

    private RenderMeta resolveAgentMeta(String id) {
        ApplicationEntity app = applicationService.lambdaQuery()
                .select(ApplicationEntity::getIcon, ApplicationEntity::getName)
                .eq(ApplicationEntity::getId, id)
                .one();
        return app == null ? null : new RenderMeta(app.getIcon(), app.getName(), "");
    }

    /**
     * 工具调用渲染所需的展示元数据
     */
    private record RenderMeta(String icon, String name, String toolType) {
        static RenderMeta fallback(String name) {
            return new RenderMeta("", name, "");
        }
    }
}
