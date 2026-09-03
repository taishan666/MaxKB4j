package com.maxkb4j.tool.executor;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.tool.sandbox.GroovySandboxCompilerConfigurer;
import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import com.maxkb4j.tool.sandbox.GroovySandboxPolicy;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 安全的 Groovy 脚本执行器。
 * <p>
 * 采用多层防护，各层由独立组件承担：
 * 1. 文本预检（{@link GroovySandboxPolicy#findDangerousToken}）：编译前粗粒度拦截脚本中的危险标记；
 * 2. 编译期防护（{@link GroovySandboxCompilerConfigurer}）：AST 限制（类引用仅限白名单、限制常量类型）
 *    与沙箱转换（注入运行期拦截点）；
 * 3. 运行期沙箱（{@link GroovySandboxInterceptor}）：只允许白名单中的类和方法调用；
 * 4. 超时控制：通过线程池限制脚本执行时间，防止无限循环/资源耗尽。
 * 编译缓存由 {@link GroovyScriptCache} 负责，本类只关注执行编排。
 * </p>
 */
@Slf4j
public class GroovyScriptExecutor extends AbsToolExecutor {

    /** 脚本执行超时时间（秒） */
    private static final int EXECUTION_TIMEOUT_SECONDS = 60;

    /** 重建共享执行器时的同步锁，保证超时替换时只有一个新池被创建 */
    private static final Object EXECUTOR_LOCK = new Object();

    /**
     * 共享的脚本执行线程池（单线程 daemon）。
     * <p>
     * 复用以避免每次执行都新建线程池；超时且脚本死循环不响应中断时，关闭旧池并重建，
     * 保证后续执行不会被卡死的脚本阻塞（每次超时至多滞留一个 daemon 线程）。
     * </p>
     */
    private static volatile ExecutorService SCRIPT_EXECUTOR = newScriptExecutor();

    private static ExecutorService scriptExecutor() {
        ExecutorService executor = SCRIPT_EXECUTOR;
        if (executor.isShutdown()) {
            synchronized (EXECUTOR_LOCK) {
                executor = SCRIPT_EXECUTOR;
                if (executor.isShutdown()) {
                    executor = newScriptExecutor();
                    SCRIPT_EXECUTOR = executor;
                }
            }
        }
        return executor;
    }

    private static ExecutorService newScriptExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "groovy-sandbox-worker");
            t.setDaemon(true);
            return t;
        });
    }

    private final String code;
    private final Map<String, Object> initParams;

    public GroovyScriptExecutor(String code, Map<String, Object> initParams) {
        this.code = code;
        this.initParams = initParams;
    }

    /** 供测试检查编译缓存命中情况 */
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
     * @param params 脚本参数
     * @return 脚本执行结果
     * @throws SecurityException 当脚本尝试调用非白名单中的类或方法时
     * @throws RuntimeException  当脚本执行超时或失败时
     */
    public Object execute(Map<String, Object> params) {
        if (StringUtils.isBlank(code)) {
            return "";
        }

        String dangerousToken = GroovySandboxPolicy.findDangerousToken(code);
        if (dangerousToken != null) {
            throw new SecurityException(I18nUtil.get("tool.groovy.script.dangerous.call", dangerousToken));
        }

        // 不直接修改调用方传入的 map：合并到新 map，initParams 保持原有覆盖语义
        Map<String, Object> mergedParams = new LinkedHashMap<>();
        if (params != null) {
            mergedParams.putAll(params);
        }
        if (initParams != null) {
            mergedParams.putAll(initParams);
        }

        // 编译结果复用：相同脚本只编译一次，每次执行新建 Script 实例保证隔离
        Binding binding = new Binding(mergedParams);
        GroovyScriptCache.CompiledScript compiled = GroovyScriptCache.get(code);

        GroovySandboxInterceptor interceptor = new GroovySandboxInterceptor();
        ExecutorService executor = scriptExecutor();
        Future<Object> future;
        while (true) {
            try {
                future = executor.submit(() -> {
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
                break;
            } catch (RejectedExecutionException e) {
                // 并发超时路径刚关闭了共享池，重建后重新提交
                executor = scriptExecutor();
            }
        }

        try {
            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            // 关闭本次提交使用的执行池，避免死循环脚本阻塞后续执行；后续调用经 scriptExecutor() 重建新池
            executor.shutdownNow();
            throw new SecurityException(I18nUtil.get("tool.groovy.script.execution.timeout", EXECUTION_TIMEOUT_SECONDS));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            SecurityException securityException = GroovySandboxPolicy.findSecurityException(cause);
            if (securityException != null) {
                throw securityException;
            }
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", cause.getMessage()), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.interrupted"), e);
        }
    }
}