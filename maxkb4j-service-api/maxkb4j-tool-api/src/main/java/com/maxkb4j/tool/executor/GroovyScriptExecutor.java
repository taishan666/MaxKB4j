package com.maxkb4j.tool.executor;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.common.executor.AbsToolExecutor;
import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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

    private static final Set<String> DANGEROUS_METHODS = Set.of(
            "exec", "execute", "start", "getRuntime",
            "forName", "loadClass", "newInstance",
            "invoke", "invokeMethod", "getMethod", "getDeclaredMethod", "getMethods", "getDeclaredMethods",
            "getField", "getDeclaredField", "getFields", "getDeclaredFields",
            "getConstructor", "getDeclaredConstructor", "getConstructors", "getDeclaredConstructors",
            "setAccessible", "getClass", "getClassLoader", "getMetaClass", "setMetaClass",
            "parseClass", "evaluate"
    );

    private static final Set<String> DANGEROUS_PROPERTIES = Set.of(
            "metaClass", "class", "classLoader", "declaringClass", "protectionDomain",
            "methods", "declaredMethods", "fields", "declaredFields",
            "constructors", "declaredConstructors", "this", "super"
    );

    static {
        CompilerConfiguration config = new CompilerConfiguration();

        // ========== 1. 导入限制 ==========
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("java.lang.Math");
        importCustomizer.addStarImports("groovy.json", "groovy.xml", "net.objecthunter.exp4j");

        // ========== 2. AST 安全限制 ==========
        SecureASTCustomizer ast = new SecureASTCustomizer();
        ast.setClosuresAllowed(true);
        ast.setDisallowedExpressions(List.of(
                ClassExpression.class,
                MethodPointerExpression.class,
                AttributeExpression.class
        ));
        ast.addExpressionCheckers(GroovyScriptExecutor::isSafeExpression);
        ast.setDisallowedImports(List.of(
                "java.lang.Runtime",
                "java.lang.Process",
                "java.lang.ProcessBuilder",
                "java.lang.System",
                "java.lang.Class",
                "java.lang.ClassLoader",
                "java.io.File",
                "java.nio.file.Files",
                "java.nio.file.Path",
                "java.net.URL",
                "java.net.URI",
                "groovy.lang.GroovyShell",
                "groovy.lang.GroovyClassLoader",
                "groovy.lang.MetaClass",
                "groovy.lang.ExpandoMetaClass"
        ));
        ast.setDisallowedStarImports(List.of(
                "java.lang.reflect",
                "java.lang.invoke",
                "java.io",
                "java.nio.file",
                "java.net"
        ));
        ast.setDisallowedStaticImports(List.of(
                "java.lang.Runtime.getRuntime",
                "java.lang.System.getenv",
                "java.lang.System.getProperty",
                "java.lang.Class.forName"
        ));
        ast.setDisallowedStaticStarImports(List.of(
                "java.lang.Runtime",
                "java.lang.System",
                "java.lang.Class",
                "java.lang.ProcessBuilder"
        ));

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

        validateScriptContent(code);

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
            SecurityException securityException = findSecurityException(cause);
            if (securityException != null) {
                throw securityException;
            }
            throw new RuntimeException("脚本执行失败: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("脚本执行被中断", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void validateScriptContent(String script) {
        String normalized = script.toLowerCase();
        for (String token : dangerousTokens()) {
            if (normalized.contains(token)) {
                throw new SecurityException("脚本包含不允许的危险调用: " + token);
            }
        }
    }

    private static Collection<String> dangerousTokens() {
        return List.of(
                "runtime", "processbuilder", "java.lang.process", "java.lang.system", "system.getenv", "system.getproperty", "class.forname",
                "getruntime", ".exec", ".execute", ".start", "getclass", "getclassloader", "loadclass",
                "metaclass", "classloader", "java.lang.reflect", "java.lang.invoke", "setaccessible",
                "getmethod", "getdeclaredmethod", "invoke(", "new file", "java.io.", "java.nio.file",
                "files.read", "path.of", "java.net.", "groovyshell", "groovyclassloader"
        );
    }

    private static boolean isSafeExpression(Expression expression) {
        if (expression instanceof MethodCallExpression methodCallExpression) {
            String methodName = methodCallExpression.getMethodAsString();
            return methodName == null || !DANGEROUS_METHODS.contains(methodName);
        }
        if (expression instanceof StaticMethodCallExpression staticMethodCallExpression) {
            return !DANGEROUS_METHODS.contains(staticMethodCallExpression.getMethod());
        }
        if (expression instanceof PropertyExpression propertyExpression) {
            String propertyName = propertyExpression.getPropertyAsString();
            return propertyName == null || !DANGEROUS_PROPERTIES.contains(propertyName);
        }
        return true;
    }

    private static SecurityException findSecurityException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof SecurityException securityException) {
                return securityException;
            }
            throwable = throwable.getCause();
        }
        return null;
    }

}

