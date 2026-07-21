package com.maxkb4j.application.tool;

import com.maxkb4j.application.dto.ApplicationSimple;
import com.maxkb4j.application.executor.AgentExecutor;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.tool.service.IAgentToolService;
import com.maxkb4j.tool.util.ToolNaming;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 应用工具服务实现，负责将智能体应用构建为可执行的工具。
 *
 * <p>本类实现 tool-api 中的 {@link IAgentToolService} SPI，由 tool 模块的
 * {@code ToolProviderServiceImpl} 在运行期注入，从而避免 tool 模块反向编译依赖 application。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ApplicationToolServiceImpl implements IAgentToolService {

    private final IApplicationService applicationService;
    private final IApplicationChatService chatService;

    /**
     * 根据应用 ID 列表构建 AiServiceTool 列表
     */
    @Override
    public List<AiServiceTool> buildTools(List<String> applicationIds) {
        return buildAiServiceTools(applicationIds);
    }

    /**
     * 根据应用 ID 列表构建 ToolProvider
     */
    @Override
    public ToolProvider buildToolProvider(List<String> applicationIds) {
        List<AiServiceTool> tools = buildAiServiceTools(applicationIds);
        if (tools.isEmpty()) {
            return null;
        }
        return toolProviderRequest -> ToolProviderResult.builder().addAll(tools).build();
    }

    /**
     * 查询应用列表并构建 AiServiceTool 列表
     */
    private List<AiServiceTool> buildAiServiceTools(List<String> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApplicationSimple> applications = queryApplications(applicationIds);
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }
        return applications.stream()
                .map(this::toAiServiceTool)
                .toList();
    }

    /**
     * 查询指定 ID 的应用列表（仅选取构建工具所需的字段）
     */
    private List<ApplicationSimple> queryApplications(List<String> applicationIds) {
        return applicationService.listAppSimpleByIds(applicationIds);
    }

    /**
     * 将单个应用实体构建为 AiServiceTool（工具规范 + 执行器）
     */
    private AiServiceTool toAiServiceTool(ApplicationSimple app) {
        ToolSpecification spec = buildAgentSpecification(app);
        ToolExecutor executor = new AgentExecutor(app.getId(), chatService);
        return AiServiceTool.builder()
                .toolSpecification(spec)
                .toolExecutor(executor)
                .build();
    }

    /**
     * 根据应用实体构建 ToolSpecification（应用作为工具时的规范）：
     * 单参 message，名称遵循 agent_&lt;id&gt; 约定。
     */
    private ToolSpecification buildAgentSpecification(ApplicationSimple app) {
        JsonObjectSchema parameters = JsonObjectSchema.builder()
                .addStringProperty("message")
                .required("message")
                .build();
        return ToolSpecification.builder()
                .name(ToolNaming.buildAgentName(app.getId()))
                .description("**" + app.getName() + "**" + ":" + app.getDesc())
                .parameters(parameters)
                .build();
    }
}
