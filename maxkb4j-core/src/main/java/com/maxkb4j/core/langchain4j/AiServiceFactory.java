package com.maxkb4j.core.langchain4j;

import com.maxkb4j.core.listener.AssistantCompletedListener;
import com.maxkb4j.core.listener.AssistantErrorListener;
import com.maxkb4j.core.listener.AssistantStartedListener;
import com.maxkb4j.core.listener.AssistantToolExecutedEventListener;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class AiServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(AiServiceFactory.class);

    private static final Collection<AiServiceListener<?>> LISTENERS = Arrays.asList(
            new AssistantStartedListener(),
            new AssistantCompletedListener(),
            new AssistantToolExecutedEventListener(),
            new AssistantErrorListener()
    );

    /**
     * 工具执行失败的统一处理器。
     *
     * <p>langchain4j 默认行为：捕获异常后打印整段 WARN 堆栈，并把原始错误信息原样回传给 LLM。
     * 对于 MCP 远程服务器返回的业务错误（如内容审核「当前输出可能包含不当内容」、限流、参数校验等），
     * 默认堆栈噪音大，且模型容易据此反复重试同一被拦截的调用。这里改为：</p>
     * <ul>
     *   <li>仅记录一行简洁告警（工具名 + 失败原因），不打印堆栈，消除默认处理器的日志噪音；</li>
     *   <li>回传可读、可操作的失败说明，引导模型更换参数/工具或直接向用户说明，避免无意义的重复重试。</li>
     * </ul>
     */
    private static final ToolExecutionErrorHandler TOOL_EXECUTION_ERROR_HANDLER = (error, context) -> {
        ToolExecutionRequest request = context.toolExecutionRequest();
        String toolName = request != null ? request.name() : "unknown";
        String reason = rootMessage(error);
        log.warn("Tool [{}] execution failed: {}", toolName, reason);
        return ToolErrorHandlerResult.text(
                "工具「" + toolName + "」调用失败：" + reason
                        + "。请勿重复完全相同的调用；可调整参数、更换工具，或直接向用户说明该工具当前不可用。");
    };

    public static <T> AiServices<T> builder(Class<T> aiService) {
        Function<ToolExecutionRequest, ToolExecutionResultMessage> strategy = (request) -> ToolExecutionResultMessage.builder()
                .id(request.id())
                .toolName(request.name())
                .text("工具（" + request.name() + "）不存在！")
                .isError(true)
                .build();
        return AiServices.builder(aiService)
                .storeRetrievedContentInChatMemory(false)
                .hallucinatedToolNameStrategy(strategy)
                .toolExecutionErrorHandler(TOOL_EXECUTION_ERROR_HANDLER)
                .registerListeners(LISTENERS);
    }

    /**
     * 提取异常链中最深层的可读信息；若均无 message 则退化为异常类名。
     */
    private static String rootMessage(Throwable error) {
        if (error == null) {
            return "未知错误";
        }
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return message;
    }

}
