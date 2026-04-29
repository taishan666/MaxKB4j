package com.maxkb4j.application.executor;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.application.sandbox.GroovySandboxInterceptor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 安全的 Groovy 脚本执行器
 * <p>
 * 采用三层防护：
 * 1. 编译期 AST 限制（SecureASTCustomizer）：禁止 .class 字面量、限制常量类型
 * 2. 运行期沙箱（SandboxTransformer + 白名单拦截器）：只允许白名单中的类和方法调用
 * 3. 超时控制：通过线程池限制脚本执行时间，防止无限循环/资源耗尽
 * </p>
 */
public class GroovyScriptExecutor extends AbsToolExecutor {

    /** 脚本执行超时时间（秒） */
    private static final int EXECUTION_TIMEOUT_SECONDS = 60;

    private static final CompilerConfiguration SAFE_CONFIG;

    static {
        CompilerConfiguration config = new CompilerConfiguration();

        // ========== 1. 导入限制 ==========
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("java.lang.Math");
        importCustomizer.addStarImports("groovy.json", "groovy.xml", "net.objecthunter.exp4j");

        // ========== 2. AST 安全限制 ==========
        SecureASTCustomizer ast = new SecureASTCustomizer();
        ast.setClosuresAllowed(true);
        ast.setDisallowedExpressions(List.of(ClassExpression.class));

        @SuppressWarnings("rawtypes")
        List<Class> allowedConstants = new ArrayList<>();
        allowedConstants.add(String.class);
        allowedConstants.add(Integer.class);
        allowedConstants.add(Long.class);
        allowedConstants.add(Double.class);
        allowedConstants.add(Float.class);
        allowedConstants.add(Boolean.class);
        allowedConstants.add(BigDecimal.class);
        allowedConstants.add(BigInteger.class);
        ast.setAllowedConstantTypesClasses(allowedConstants);

        // ========== 3. Groovy Sandbox 运行期沙箱 ==========
        // SandboxTransformer 默认启用所有拦截：方法、构造函数、属性、数组、属性访问
        SandboxTransformer sandboxTransformer = new SandboxTransformer();

        // ========== 4. 组合配置 ==========
        config.addCompilationCustomizers(importCustomizer, ast, sandboxTransformer);
        config.setScriptBaseClass("groovy.lang.Script");
        config.setDisabledGlobalASTTransformations(Set.of("Grab", "GrabConfig", "GrabResolver"));

        SAFE_CONFIG = config;
    }

    private final String code;
    private final Map<String, Object> initParams;

    public GroovyScriptExecutor(String code, Map<String, Object> initParams) {
        this.code = code;
        this.initParams = initParams;
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

        if (initParams != null) {
            params.putAll(initParams);
        }

        Binding binding = new Binding(params);
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader(
                GroovyScriptExecutor.class.getClassLoader(), SAFE_CONFIG);
        GroovyShell shell = new GroovyShell(groovyClassLoader, binding);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "groovy-sandbox-worker");
            t.setDaemon(true);
            return t;
        });

        GroovySandboxInterceptor interceptor = new GroovySandboxInterceptor();
        try {
            Future<Object> future = executor.submit(() -> {
                // 注册白名单拦截器（SandboxTransformer 已将调用注入到字节码中）
                interceptor.register();
                try {
                    Object result = shell.evaluate(code);
                    return result == null ? "" : result;
                } finally {
                    interceptor.unregister();
                }
            });

            return future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            throw new SecurityException("脚本执行超时（超过 " + EXECUTION_TIMEOUT_SECONDS + " 秒），已强制终止");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                throw (SecurityException) cause;
            }
            throw new RuntimeException("脚本执行失败: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("脚本执行被中断", e);
        } finally {
            executor.shutdownNow();
        }
    }


}
