package com.maxkb4j.tool.executor;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.MD5Util;
import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
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
@Slf4j
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

        // 注意：Groovy 4 的 SecureASTCustomizer.visitVariableExpression 会复用该白名单校验
        // 变量的静态类型，类型不在名单中的脚本变量会在编译期被拒绝，而脚本绑定变量与
        // def 声明变量的推断类型均为 java.lang.Object，因此名单必须覆盖 Object、基本类型
        // 及运行期白名单（GroovySandboxInterceptor）中的常见安全类型，否则正常业务脚本会被误杀。
        @SuppressWarnings("rawtypes")
        List<Class> allowedConstants = new ArrayList<>();
        // 脚本绑定变量 / 闭包参数的默认静态类型
        allowedConstants.add(Object.class);
        // 基本类型（int x = 5 一类的显式类型声明与字面量）
        allowedConstants.add(int.class);
        allowedConstants.add(long.class);
        allowedConstants.add(double.class);
        allowedConstants.add(float.class);
        allowedConstants.add(boolean.class);
        allowedConstants.add(char.class);
        allowedConstants.add(byte.class);
        allowedConstants.add(short.class);
        // 字面量常量类型
        allowedConstants.add(String.class);
        allowedConstants.add(Integer.class);
        allowedConstants.add(Long.class);
        allowedConstants.add(Double.class);
        allowedConstants.add(Float.class);
        allowedConstants.add(Boolean.class);
        allowedConstants.add(Character.class);
        allowedConstants.add(BigDecimal.class);
        allowedConstants.add(BigInteger.class);
        // 显式类型声明常用类型（与运行期白名单 GroovySandboxInterceptor 保持一致）
        allowedConstants.add(Number.class);
        allowedConstants.add(java.util.Collection.class);
        allowedConstants.add(java.util.List.class);
        allowedConstants.add(java.util.ArrayList.class);
        allowedConstants.add(java.util.LinkedList.class);
        allowedConstants.add(java.util.Map.class);
        allowedConstants.add(java.util.HashMap.class);
        allowedConstants.add(java.util.LinkedHashMap.class);
        allowedConstants.add(java.util.Set.class);
        allowedConstants.add(java.util.HashSet.class);
        allowedConstants.add(java.util.LinkedHashSet.class);
        allowedConstants.add(StringBuilder.class);
        allowedConstants.add(StringBuffer.class);
        allowedConstants.add(groovy.lang.GString.class);
        allowedConstants.add(java.util.Date.class);
        allowedConstants.add(java.time.LocalDate.class);
        allowedConstants.add(java.time.LocalDateTime.class);
        allowedConstants.add(java.time.LocalTime.class);
        allowedConstants.add(java.time.Instant.class);
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

    /** 编译缓存最大条目数，超出按 LRU 淘汰 */
    private static final int MAX_CACHED_SCRIPTS = 256;

    /**
     * 已编译脚本缓存：key 为脚本内容摘要，value 为脚本类及其专属 ClassLoader。
     * <p>
     * Groovy 每次编译都会生成新的 Class，重复编译既浪费 CPU 又导致 metaspace 增长。
     * 相同脚本只编译一次，后续执行仅新建 Script 实例（实例独立、线程安全）。
     * 每个唯一脚本使用独立 GroovyClassLoader，缓存淘汰后整个 loader 连同其加载的类可被 GC 回收。
     * </p>
     */
    private static final Map<String, CompiledScript> SCRIPT_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<String, CompiledScript>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CompiledScript> eldest) {
                    return size() > MAX_CACHED_SCRIPTS;
                }
            });

    private record CompiledScript(GroovyClassLoader loader, Class<?> scriptClass) {
    }

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
        return SCRIPT_CACHE.containsKey(cacheKey(code));
    }

    private static String cacheKey(String code) {
        return MD5Util.encrypt(code);
    }

    private static CompiledScript cachedScript(String code) {
        synchronized (SCRIPT_CACHE) {
            return SCRIPT_CACHE.computeIfAbsent(cacheKey(code), key -> compileScript(code));
        }
    }

    private static CompiledScript compileScript(String code) {
        GroovyClassLoader loader = new GroovyClassLoader(GroovyScriptExecutor.class.getClassLoader(), SAFE_CONFIG);
        Class<?> scriptClass;
        try {
            scriptClass = loader.parseClass(code);
        } catch (CompilationFailedException e) {
            // SecureAST 在编译期抛出的 SecurityException 会被包装进编译失败异常，
            // 这里还原原始 SecurityException，保持与运行期沙箱拒绝一致的错误语义
            SecurityException securityException = findCompileSecurityException(e);
            if (securityException != null) {
                throw securityException;
            }
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", e.getMessage()), e);
        }
        if (!Script.class.isAssignableFrom(scriptClass)) {
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", "unsupported script structure"));
        }
        return new CompiledScript(loader, scriptClass);
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

        validateScriptContent(code);

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
        CompiledScript compiled = cachedScript(code);

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
            // 关闭本次提交所在的共享池：脚本响应中断则线程立即回收；
            // 死循环不响应中断时该 daemon 线程滞留，下一次执行会通过 scriptExecutor() 重建新池
            executor.shutdownNow();
            throw new SecurityException(I18nUtil.get("tool.groovy.script.execution.timeout", EXECUTION_TIMEOUT_SECONDS));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            SecurityException securityException = findSecurityException(cause);
            if (securityException != null) {
                throw securityException;
            }
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", cause.getMessage()), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.interrupted"), e);
        }
    }

    private static void validateScriptContent(String script) {
        String normalized = script.toLowerCase();
        for (String token : dangerousTokens()) {
            if (normalized.contains(token)) {
                throw new SecurityException(I18nUtil.get("tool.groovy.script.dangerous.call", token));
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

    /**
     * 从编译失败异常中提取 SecureASTCustomizer 抛出的 SecurityException。
     * 编译期安全拒绝会被逐层包装为 MultipleCompilationErrorsException，
     * 原始异常保存在错误收集器的 ExceptionMessage 中。
     */
    private static SecurityException findCompileSecurityException(CompilationFailedException exception) {
        SecurityException fromCauseChain = findSecurityException(exception);
        if (fromCauseChain != null) {
            return fromCauseChain;
        }
        if (exception instanceof MultipleCompilationErrorsException multi) {
            ErrorCollector collector = multi.getErrorCollector();
            for (int i = 0; collector != null && i < collector.getErrorCount(); i++) {
                Message message = collector.getError(i);
                if (message instanceof ExceptionMessage exceptionMessage) {
                    SecurityException found = findSecurityException(exceptionMessage.getCause());
                    if (found != null) {
                        return found;
                    }
                }
            }
            // 兜底：cause 链丢失时依据错误文本判定安全拒绝
            String message = multi.getMessage();
            if (message != null && message.contains(SecurityException.class.getName())) {
                return new SecurityException(message);
            }
        }
        return null;
    }

}

