package com.maxkb4j.application.executor;

import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.*;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

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
 * 1. 编译期 AST 限制（SecureASTCustomizer）：白名单导入、禁止 .class 字面量、限制常量类型
 * 2. 运行期 MetaClass 沙箱：替换核心类的 MetaClass 拦截危险方法调用
 * 3. 超时控制：通过线程池限制脚本执行时间，防止无限循环/资源耗尽
 * </p>
 */
public class GroovyScriptExecutor extends AbsToolExecutor {

    /** 脚本执行超时时间（秒） */
    private static final int EXECUTION_TIMEOUT_SECONDS = 30;

    private static final CompilerConfiguration SAFE_CONFIG;

    /** 禁止使用的危险类（全限定名） */
    private static final Set<String> BLOCKED_CLASSES = Set.of(
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "java.lang.System",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Thread",
            "java.lang.reflect.Proxy",
            "java.lang.reflect.Method",
            "java.lang.reflect.Field",
            "java.lang.reflect.Constructor",
            "groovy.lang.GroovyShell",
            "groovy.lang.GroovyClassLoader",
            "groovy.lang.Script",
            "groovy.util.Eval",
            "groovy.util.GroovyScriptEngine",
            "javax.script.ScriptEngine",
            "javax.script.ScriptEngineManager",
            "java.security.AccessController",
            "sun.misc.Unsafe",
            "jdk.internal.misc.Unsafe",
            "java.lang.invoke.MethodHandle",
            "java.beans.EventHandler",
            "java.lang.instrument.Instrumentation",
            "java.net.URLClassLoader",
            "java.security.SecureClassLoader"
    );

    /** 禁止调用的方法名 */
    private static final Set<String> BLOCKED_METHODS = Set.of(
            "exec", "getRuntime", "exit", "halt",
            "getClass", "getClassLoader",
            "getDeclaredFields", "getDeclaredMethods",
            "getDeclaredConstructors", "getDeclaredField",
            "getDeclaredMethod", "newInstance", "forName",
            "setAccessible", "invoke", "defineClass",
            "execute"
    );

    static {
        CompilerConfiguration config = new CompilerConfiguration();
        // ========== 1. 导入限制 ==========
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("java.lang.Math");
        importCustomizer.addStarImports("groovy.json", "groovy.xml", "groovy.sql", "groovy.lang", "groovy.util", "groovy.time",
                "net.objecthunter.exp4j");

        // ========== 2. AST 安全限制 ==========
        SecureASTCustomizer ast = new SecureASTCustomizer();
        ast.setClosuresAllowed(true);
        ast.setDisallowedExpressions(List.of(ClassExpression.class));

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

        // ========== 3. 组合配置 ==========
        config.addCompilationCustomizers(importCustomizer, ast);
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
     * 执行 Groovy 脚本（带 MetaClass 沙箱隔离和超时控制）
     *
     * @param params 脚本参数
     * @return 脚本执行结果
     * @throws SecurityException 当脚本尝试执行危险操作时
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
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader(GroovyScriptExecutor.class.getClassLoader(), SAFE_CONFIG);
        GroovyShell shell = new GroovyShell(groovyClassLoader, binding);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "groovy-sandbox-worker");
            t.setDaemon(true);
            return t;
        });

        try {
            Future<Object> future = executor.submit(() -> {
                MetaClassRegistrySandbox sandbox = new MetaClassRegistrySandbox();
                sandbox.install();
                try {
                    Object result = shell.evaluate(code);
                    return result == null ? "" : result;
                } finally {
                    sandbox.uninstall();
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

    /**
     * 基于 Groovy MetaClassRegistry 的运行期沙箱。
     * 在 Groovy 4.x 中替代不兼容的 groovy-sandbox 库，
     * 通过 DelegatingMetaClass 替换核心类的 MetaClass 来拦截危险方法调用。
     */
    private static class MetaClassRegistrySandbox {

        private final Map<Class<?>, MetaClass> originalMetaClasses = new ConcurrentHashMap<>();
        private final Map<Class<?>, DelegatingMetaClass> sandboxMetaClasses = new ConcurrentHashMap<>();

        private static final Class<?>[] INTERCEPTED_CLASSES = {
                Object.class,
                Class.class,
                String.class,
                System.class
        };

        void install() {
            for (Class<?> clazz : INTERCEPTED_CLASSES) {
                MetaClass original = GroovySystem.getMetaClassRegistry().getMetaClass(clazz);
                originalMetaClasses.put(clazz, original);

                DelegatingMetaClass sandboxMetaClass = createSandboxMetaClass(original);
                sandboxMetaClasses.put(clazz, sandboxMetaClass);
                GroovySystem.getMetaClassRegistry().setMetaClass(clazz, sandboxMetaClass);
            }
        }

        void uninstall() {
            for (Map.Entry<Class<?>, MetaClass> entry : originalMetaClasses.entrySet()) {
                GroovySystem.getMetaClassRegistry().setMetaClass(entry.getKey(), entry.getValue());
            }
            sandboxMetaClasses.clear();
        }

        private DelegatingMetaClass createSandboxMetaClass(MetaClass delegate) {
            return new DelegatingMetaClass(delegate) {
                @Override
                public Object invokeMethod(Object object, String methodName, Object[] arguments) {
                    checkDangerous(object, methodName);
                    return super.invokeMethod(object, methodName, arguments);
                }

                @Override
                public Object invokeMethod(Object object, String methodName, Object arguments) {
                    checkDangerous(object, methodName);
                    return super.invokeMethod(object, methodName, arguments);
                }

                @Override
                public Object invokeStaticMethod(Object object, String methodName, Object[] arguments) {
                    checkDangerous(object, methodName);
                    return super.invokeStaticMethod(object, methodName, arguments);
                }

                @Override
                public Object invokeConstructor(Object[] arguments) {
                    String className = getTheClass().getName();
                    if (BLOCKED_CLASSES.contains(className)) {
                        throw new SecurityException("不允许构造类: " + className);
                    }
                    return super.invokeConstructor(arguments);
                }

                private void checkDangerous(Object receiver, String methodName) {
                    if (receiver == null) return;
                    String className = receiver.getClass().getName();
                    if (className.contains("$$")) {
                        className = className.substring(0, className.indexOf("$$"));
                    }
                    if (BLOCKED_CLASSES.contains(className)) {
                        throw new SecurityException("不允许在类 " + className + " 上执行方法: " + methodName);
                    }
                    if (BLOCKED_METHODS.contains(methodName)) {
                        throw new SecurityException("不允许调用方法: " + methodName);
                    }
                }
            };
        }
    }
}
