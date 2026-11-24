package com.maxkb4j.tool.executor;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.tool.sandbox.GroovySandboxCompilerConfigurer;
import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import com.maxkb4j.tool.sandbox.GroovySandboxPolicy;
import com.maxkb4j.tool.sandbox.GroovyScriptCache;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.Binding;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 安全的 Groovy 脚本执行器。
 * <p>
 * 采用多层防护，各层由独立组件承担：
 * 1. 文本预检（{@link GroovySandboxPolicy#findDangerousToken}）：编译前粗粒度拦截脚本中的危险标记；
 * 2. 编译期防护（{@link GroovySandboxCompilerConfigurer}）：AST 限制（类引用仅限白名单、限制常量类型）
 * 与沙箱转换（注入运行期拦截点）；
 * 3. 运行期沙箱（{@link GroovySandboxInterceptor}）：只允许白名单中的类和方法调用；
 * 4. 超时控制：通过线程池限制脚本执行时间，防止无限循环/资源耗尽。
 * 编译缓存由 {@link GroovyScriptCache} 负责，本类只关注执行编排。
 * </p>
 */
@Slf4j
public class GroovyScriptExecutor extends AbsToolExecutor {

    /**
     * 脚本执行超时时间（秒）
     */
    private static final int EXECUTION_TIMEOUT_SECONDS = 60;

    /**
     * 共享的固定单线程执行池（daemon）。
     * <p>
     * 超时只中断当前任务，绝不关闭并重建执行池，避免不响应中断的脚本不断遗留 Worker。
     * 编译期注入的 ThreadInterrupt 检查保证 Groovy 循环和方法能够响应此处的中断。
     * </p>
     */
    private static final ExecutorService SCRIPT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "groovy-sandbox-worker");
        t.setDaemon(true);
        return t;
    });
    private final String code;
    private final Map<String, Object> initParams;
    private final Map<String, Object> contextBindings;
    private final long executionTimeoutMillis;

    public GroovyScriptExecutor(String code, Map<String, Object> initParams) {
        this(code, initParams, Map.of());
    }

    /**
     * 构造执行器，并将 Spring 注入的 Bean 以绑定变量形式提供给脚本，
     * 替代脚本内通过静态工具类从容器拉取 Bean（服务定位器反模式）。
     *
     * @param contextBindings 暴露给脚本的 Bean 绑定（绑定变量名 -> 实例）
     */
    public GroovyScriptExecutor(String code, Map<String, Object> initParams, Map<String, Object> contextBindings) {
        this(code, initParams, contextBindings, TimeUnit.SECONDS.toMillis(EXECUTION_TIMEOUT_SECONDS));
    }

    GroovyScriptExecutor(String code, Map<String, Object> initParams, long executionTimeoutMillis) {
        this(code, initParams, Map.of(), executionTimeoutMillis);
    }

    GroovyScriptExecutor(String code, Map<String, Object> initParams, Map<String, Object> contextBindings, long executionTimeoutMillis) {
        this.code = code;
        this.initParams = initParams;
        this.contextBindings = contextBindings;
        this.executionTimeoutMillis = executionTimeoutMillis;
    }

    /**
     * 供测试检查编译缓存命中情况
     */
    static boolean isScriptCached(String code) {
        return GroovyScriptCache.contains(code);
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> params = argumentsAsMap(toolExecutionRequest.arguments());
        Object value = execute(params);
        return JSON.toJSONString(value);
    }

    /**
     * 执行 Groovy 脚本（带沙箱隔离和超时控制）
     *
     * @param inputParams 脚本参数
     * @return 脚本执行结果
     * @throws SecurityException 当脚本尝试调用非白名单中的类或方法时
     * @throws RuntimeException  当脚本执行超时或失败时
     */
    public Object execute(Map<String, Object> inputParams) {
        if (StringUtils.isBlank(code)) {
            return "";
        }

        String dangerousToken = GroovySandboxPolicy.findDangerousToken(code);
        if (dangerousToken != null) {
            throw new SecurityException(I18nUtil.get("tool.groovy.script.dangerous.call", dangerousToken));
        }

        // 不直接修改调用方传入的 map：合并到新 map，initParams 保持原有覆盖语义。
        // Bean 绑定作为最低优先级并入，脚本变量与 initParams 仍可覆盖同名绑定
        Map<String, Object> mergedParams = new LinkedHashMap<>();
        if (contextBindings != null) {
            mergedParams.putAll(contextBindings);
        }
        if (inputParams != null) {
            mergedParams.putAll(inputParams);
        }
        if (initParams != null) {
            mergedParams.putAll(initParams);
        }

        // 编译结果复用：相同脚本只编译一次，每次执行新建 Script 实例保证隔离
        Binding binding = new Binding(mergedParams);
        GroovyScriptCache.CompiledScript compiled = GroovyScriptCache.get(code);

        GroovySandboxInterceptor interceptor = new GroovySandboxInterceptor();
        Future<Object> future = SCRIPT_EXECUTOR.submit(() -> {
            // 注册白名单拦截器（SandboxTransformer 已将调用注入到字节码中）
            interceptor.register();
            try {
                Script script = (Script) compiled.scriptClass().getDeclaredConstructor().newInstance();
                script.setBinding(binding);
                Object result = script.run();
                return result == null ? "" : result;
            } finally {
                interceptor.unregister();
            }
        });

        try {
            return future.get(executionTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SecurityException(I18nUtil.get("tool.groovy.script.execution.timeout", EXECUTION_TIMEOUT_SECONDS));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            SecurityException securityException = GroovySandboxPolicy.findSecurityException(cause);
            if (securityException != null) {
                throw securityException;
            }
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", cause.getMessage()), cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.interrupted"), e);
        }
    }
}


