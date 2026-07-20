package com.maxkb4j.application.tool;

import com.maxkb4j.application.dto.ApplicationSimple;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.tool.service.IAgentToolMetaResolver;
import com.maxkb4j.tool.vo.ToolRenderMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * agent 类工具调用展示元数据解析实现。
 *
 * <p>实现 tool-api 中的 {@link IAgentToolMetaResolver} SPI，由 tool 模块的
 * {@code ToolFormatterService} 在运行期注入，查询应用图标/名称用于渲染 agent 工具调用。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class AgentToolMetaResolver implements IAgentToolMetaResolver {

    private final IApplicationService applicationService;

    @Override
    public ToolRenderMeta resolve(String agentId) {
        ApplicationSimple app = applicationService.getAppSimpleById(agentId);
        return app == null ? null : new ToolRenderMeta(app.getIcon(), app.getName());
    }
}
