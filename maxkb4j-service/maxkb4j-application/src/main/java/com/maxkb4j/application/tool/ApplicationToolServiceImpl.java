package com.maxkb4j.application.tool;

import com.maxkb4j.application.dto.ApplicationSimple;
import com.maxkb4j.application.executor.AgentExecutor;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationInternalService;
import com.maxkb4j.tool.service.IAgentToolService;
import com.maxkb4j.tool.util.ToolNaming;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 应用工具服务实现，负责将智能体应用构建为可执行的工具。
 *
 * <p>本类实现 tool-api 中的 {@link IAgentToolService} SPI，由 tool 模块的
 * {@code ToolProviderServiceImpl} 在运行期注入，从而避免 tool 模块反向编译依赖 application。
 *
 * <p>注入的 {@code chatService} 带 {@link Lazy}：应用作为工具执行时会回调聊天服务，
 * 与「聊天服务 → ChatServiceBuilder → 聊天管线 → 工具」构成构造期循环依赖；
 * 该依赖仅在工具执行期使用，延迟到首次调用时解析即可打破闭环。
 *
 * @author tarzan
 */
@Service
public class ApplicationToolServiceImpl implements IAgentToolService {

    private final IApplicationInternalService applicationService;
    private final IApplicationChatService chatService;

    public ApplicationToolServiceImpl(IApplicationInternalService applicationService,
                                      @Lazy IApplicationChatService chatService) {
        this.applicationService = applicationService;
        this.chatService = chatService;
    }

    /**
     * 根据应用 ID 列表构建 AiServiceTool 列表
     */
    @Override
    public List<AiServiceTool> buildTools(List<String> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApplicationSimple> applications = applicationService.listAppSimpleByIds(applicationIds);
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }
        return applications.stream()
                .map(this::toAiServiceTool)
                .toList();
    }

    /**
     * 根据应用 ID 列表构建 ToolProvider
     */
    @Override
    public ToolProvider buildToolProvider(List<String> applicationIds) {
        return toolProviderRequest -> {
            List<AiServiceTool> tools = buildTools(applicationIds);
            if (tools.isEmpty()) {
                return null;
            }
            return ToolProviderResult.builder().addAll(tools).build();
        };
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
                .addStringProperty("message", "user input")
                .required("message")
                .build();
        return ToolSpecification.builder()
                .name(ToolNaming.buildAgentName(app.getId()))
                .description("**" + app.getName() + "**" + ":" + app.getDesc())
                .parameters(parameters)
                .build();
    }
}
