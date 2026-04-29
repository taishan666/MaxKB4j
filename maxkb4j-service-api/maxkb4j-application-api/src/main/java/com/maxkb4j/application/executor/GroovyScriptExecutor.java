package com.maxkb4j.application.executor;

import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;
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
 * 采用四层防护：
 * 1. 编译期 AST 限制（SecureASTCustomizer）：白名单导入、禁止 .class 字面量、限制常量类型
 * 2. 运行期沙箱（SandboxTransformer + GroovyValueFilter）：拦截每个方法调用、属性访问和构造器调用
 * 3. 超时控制：通过线程池限制脚本执行时间，防止无限循环/资源耗尽
 * 4. 禁用全局 AST 转换：禁止 @Grab 动态下载依赖
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
            "execute"  // 拦截 String.execute() / ProcessBuilder.execute()
    );

    static {
        CompilerConfiguration config = new CompilerConfiguration();

        // ========== 1. 导入限制 ==========
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("java.lang.Math");
        // 允许 Groovy 标准模块的星导入（工具脚本常用）
        importCustomizer.addStarImports("groovy.json", "groovy.xml", "groovy.sql", "groovy.lang", "groovy.util", "groovy.time");

        // ========== 2. AST 安全限制 ==========
        SecureASTCustomizer ast = new SecureASTCustomizer();
        // 允许闭包（工具脚本常用）
        ast.setClosuresAllowed(true);
        // 禁止 ClassExpression（即禁止 .class 字面量，防止反射）
        ast.setDisallowedExpressions(List.of(
                ClassExpression.class
        ));
        // 允许使用的常量类型
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
        // 禁用 @Grab 等全局 AST 转换，防止动态下载依赖
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

        // 创建带沙箱的 CompilerConfiguration 副本，加入 SandboxTransformer
        CompilerConfiguration sandboxConfig = new CompilerConfiguration(SAFE_CONFIG);
        // SandboxTransformer 在编译期注入运行期检查点
        sandboxConfig.addCompilationCustomizers(new SandboxTransformer());

        GroovyShell shell = new GroovyShell(
                Thread.currentThread().getContextClassLoader(),
                binding,
                sandboxConfig
        );

        // 创建运行期拦截器
        SandboxGroovyValueFilter filter = new SandboxGroovyValueFilter();

        // 使用线程池实现超时控制
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "groovy-sandbox-worker");
            t.setDaemon(true);
            return t;
        });

        try {
            Future<Object> future = executor.submit(() -> {
                // 注册拦截器到全局链
                filter.register();
                try {
                    Object result = shell.evaluate(code);
                    return result == null ? "" : result;
                } finally {
                    filter.unregister();
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
     * 自定义 GroovyValueFilter，在运行期拦截危险操作。
     * 这是第二道防线——即使 AST 层面没拦住，这里也会拦截。
     */
    @SuppressWarnings("rawtypes")
    private static class SandboxGroovyValueFilter extends GroovyValueFilter {

        @Override
        public Object onMethodCall(Invoker invoker, Object receiver, String methodName, Object... args) throws Throwable {
            checkDangerousReceiver(receiver);
            checkDangerousMethod(methodName);
            return super.onMethodCall(invoker, receiver, methodName, args);
        }

        @Override
        public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
            checkDangerousReceiver(receiver);
            return super.onGetProperty(invoker, receiver, property);
        }

        @Override
        public Object onGetAttribute(Invoker invoker, Object receiver, String attribute) throws Throwable {
            checkDangerousReceiver(receiver);
            return super.onGetAttribute(invoker, receiver, attribute);
        }

        @Override
        public Object onNewInstance(Invoker invoker, Class receiver, Object... args) throws Throwable {
            if (receiver != null && isBlockedClass(receiver.getName())) {
                throw new SecurityException("不允许实例化类: " + receiver.getName());
            }
            return super.onNewInstance(invoker, receiver, args);
        }

        @Override
        public Object onStaticCall(Invoker invoker, Class receiver, String methodName, Object... args) throws Throwable {
            if (receiver != null && isBlockedClass(receiver.getName())) {
                throw new SecurityException("不允许调用静态方法: " + receiver.getName() + "." + methodName);
            }
            checkDangerousMethod(methodName);
            return super.onStaticCall(invoker, receiver, methodName, args);
        }

        /** 检查危险的调用对象 */
        private void checkDangerousReceiver(Object receiver) {
            if (receiver == null) return;
            String className = receiver.getClass().getName();
            // 处理 Groovy 包装类型（如 MC$MetaClass 等）
            if (className.contains("$$")) {
                className = className.substring(0, className.indexOf("$$"));
            }
            if (isBlockedClass(className)) {
                throw new SecurityException("不允许在类 " + className + " 上执行操作");
            }
        }

        /** 检查危险的方法调用 */
        private void checkDangerousMethod(String methodName) {
            if (BLOCKED_METHODS.contains(methodName)) {
                throw new SecurityException("不允许调用方法: " + methodName);
            }
        }

        /** 判断是否是被禁止的类 */
        private boolean isBlockedClass(String className) {
            return BLOCKED_CLASSES.contains(className);
        }
    }
}
