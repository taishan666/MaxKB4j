package com.maxkb4j.tool.sandbox;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Groovy 沙箱编译期配置器。
 * <p>
 * 负责构建加固后的 {@link CompilerConfiguration}（编译期防护层），
 * 并提供编译期安全拒绝的异常还原能力。所有类/方法白名单判定
 * 统一委托 {@link GroovySandboxPolicy}，与运行期拦截器共用同一事实来源。
 * </p>
 * <p>
 * 编译期防护包含两部分：
 * 1. AST 限制（SecureASTCustomizer）：类引用仅限白名单、限制常量类型、禁止危险导入；
 * 2. 沙箱转换（SandboxTransformer）：向字节码注入运行期拦截点，
 *    交由 {@link GroovySandboxInterceptor} 在运行期执行白名单校验。
 * </p>
 */
public final class GroovySandboxCompilerConfigurer {

    private static final CompilerConfiguration SAFE_CONFIG;

    static {
        CompilerConfiguration config = new CompilerConfiguration();

        // ========== 1. 导入限制 ==========
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("java.lang.Math");
        importCustomizer.addStarImports("groovy.json", "groovy.xml", "net.objecthunter.exp4j","java.nio.file");

        // ========== 2. AST 安全限制 ==========
        SecureASTCustomizer ast = new SecureASTCustomizer();
        ast.setClosuresAllowed(true);
        ast.setDisallowedExpressions(List.of(
                MethodPointerExpression.class,
                AttributeExpression.class
        ));
        ast.addExpressionCheckers(GroovySandboxCompilerConfigurer::isSafeExpression);
        ast.setDisallowedImports(List.of(
                "java.lang.Runtime",
                "java.lang.Process",
                "java.lang.ProcessBuilder",
                "java.lang.System",
                "java.lang.Class",
                "java.lang.ClassLoader",
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
        // 及运行期白名单（GroovySandboxPolicy）中的常见安全类型，否则正常业务脚本会被误杀。
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
        // ========== 新增：数组类型支持 ==========
        // byte[] 是文件读写、编解码、加密等操作的常用类型，必须加入白名单
        allowedConstants.add(byte[].class);
        // 建议同时添加其他基本类型数组，避免后续类似报错
        allowedConstants.add(int[].class);
        allowedConstants.add(long[].class);
        allowedConstants.add(double[].class);
        allowedConstants.add(float[].class);
        allowedConstants.add(boolean[].class);
        allowedConstants.add(char[].class);
        allowedConstants.add(short[].class);
        // String 数组也常用于参数传递
        allowedConstants.add(String[].class);
        // Object 数组用于通用集合转换
        allowedConstants.add(Object[].class);
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
        // 显式类型声明常用类型（与运行期白名单 GroovySandboxPolicy 保持一致）
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
        allowedConstants.add(Exception.class);
        // catch 具体异常类型（如 catch (DateTimeParseException e)）：异常参数的静态类型
        // 同样按精确类名校验，需逐个登记，否则编译期报
        // "Usage of variables of type [...] is not allowed"
        allowedConstants.add(Throwable.class);
        allowedConstants.add(RuntimeException.class);
        allowedConstants.add(IllegalArgumentException.class);
        allowedConstants.add(NumberFormatException.class);
        allowedConstants.add(java.time.DateTimeException.class);
        allowedConstants.add(java.time.format.DateTimeParseException.class);
        allowedConstants.add(groovy.lang.GString.class);
        allowedConstants.add(java.util.Date.class);
        allowedConstants.add(java.time.LocalDate.class);
        allowedConstants.add(java.time.LocalDateTime.class);
        allowedConstants.add(java.time.LocalTime.class);
        allowedConstants.add(java.time.Instant.class);
        allowedConstants.add(java.time.ZonedDateTime.class);
        allowedConstants.add(java.time.ZoneId.class);
        allowedConstants.add(java.time.format.DateTimeFormatter.class);
        allowedConstants.add(java.nio.file.Files.class);
        allowedConstants.add(java.nio.file.Path.class);
        allowedConstants.add(io.github.mymonstercat.ocr.InferenceEngine.class);
        allowedConstants.add(com.maxkb4j.oss.service.IOssService.class);
        allowedConstants.add(com.maxkb4j.common.util.SpringUtil.class);
        allowedConstants.add(io.github.mymonstercat.Model.class);
        allowedConstants.add(com.benjaminwan.ocrlibrary.OcrResult.class);
        // 数学表达式求值引擎（内置工具「数学公式执行」）：允许作为脚本变量类型
        // （如 Expression engine = new ExpressionBuilder(...).build()）
        allowedConstants.add(net.objecthunter.exp4j.Expression.class);
        allowedConstants.add(net.objecthunter.exp4j.ExpressionBuilder.class);
        // fastjson（内置工具 JSON 处理）：允许作为脚本变量类型，否则编译期报
        // "Usage of variables of type [com.alibaba.fastjson.JSONArray] is not allowed"
        allowedConstants.add(com.alibaba.fastjson.JSON.class);
        allowedConstants.add(com.alibaba.fastjson.JSONObject.class);
        allowedConstants.add(com.alibaba.fastjson.JSONArray.class);
        ast.setAllowedConstantTypesClasses(allowedConstants);

        // ========== 3. Groovy Sandbox 运行期沙箱 ==========
        // SandboxTransformer 默认启用所有拦截：方法、构造函数、属性、数组、属性访问
        SandboxTransformer sandboxTransformer = new SandboxTransformer();

        // ========== 4. 组合配置 ==========
        config.addCompilationCustomizers(importCustomizer, ast, sandboxTransformer);
        config.setScriptBaseClass("groovy.lang.Script");
        // 禁用 Grape 相关 AST 转换，避免运行期联网下载依赖。
        // 注意：Groovy 按 META-INF/services/org.codehaus.groovy.transform.ASTTransformation
        // 中注册的全限定类名精确匹配，写 "Grab" 等注解名不会生效；
        // @Grab/@GrabConfig/@GrabResolver 均由 GrabAnnotationTransformation 处理，
        // 未禁用时会被转换为运行期 Grape.grab(...) 静态调用。
        config.setDisabledGlobalASTTransformations(Set.of("groovy.grape.GrabAnnotationTransformation"));
        SAFE_CONFIG = config;
    }

    private GroovySandboxCompilerConfigurer() {
    }

    /** 沙箱脚本共用的安全编译配置（全局单例，禁止外部修改）。 */
    public static CompilerConfiguration safeConfiguration() {
        return SAFE_CONFIG;
    }

    /**
     * SecureASTCustomizer 表达式白名单校验，判定规则全部来自 {@link GroovySandboxPolicy}。
     */
    private static boolean isSafeExpression(Expression expression) {
        if (expression instanceof ClassExpression classExpression) {
            // 类引用（静态调用接收者、instanceof、.class 等）仅允许运行期白名单中的类，
            // 未在白名单中的类在编译期即被拒绝，避免放开 ClassExpression 后引入任意类引用
            return GroovySandboxPolicy.isAllowedClassName(classExpression.getType().getName());
        }
        if (expression instanceof MethodCallExpression methodCallExpression) {
            String methodName = methodCallExpression.getMethodAsString();
            if (methodName == null) {
                return true;
            }
            // 带接收者静态类型的危险方法校验：受信例外组合（如 exp4j Expression#evaluate）放行；
            // 接收者静态类型未知（Object）时编译期无法判定，交由运行期按实际接收者类型拦截
            Expression receiver = methodCallExpression.getObjectExpression();
            ClassNode receiverNode = receiver == null ? null : receiver.getType();
            Class<?> receiverType = receiverNode == null ? null : receiverNode.getTypeClass();
            if (receiverType == null || receiverType == Object.class) {
                return true;
            }
            return !GroovySandboxPolicy.isDangerousMethod(receiverType, methodName);
        }
        if (expression instanceof StaticMethodCallExpression staticMethodCallExpression) {
            return !GroovySandboxPolicy.isDangerousMethod(staticMethodCallExpression.getMethod());
        }
        if (expression instanceof PropertyExpression propertyExpression) {
            String propertyName = propertyExpression.getPropertyAsString();
            return propertyName == null || !GroovySandboxPolicy.isDangerousProperty(propertyName);
        }
        return true;
    }

    /**
     * 从编译失败异常中提取 SecureASTCustomizer 抛出的 SecurityException。
     * 编译期安全拒绝会被逐层包装为 MultipleCompilationErrorsException，
     * 原始异常保存在错误收集器的 ExceptionMessage 中。
     */
    public static SecurityException findCompileSecurityException(CompilationFailedException exception) {
        SecurityException fromCauseChain = GroovySandboxPolicy.findSecurityException(exception);
        if (fromCauseChain != null) {
            return fromCauseChain;
        }
        if (exception instanceof MultipleCompilationErrorsException multi) {
            ErrorCollector collector = multi.getErrorCollector();
            for (int i = 0; collector != null && i < collector.getErrorCount(); i++) {
                Message message = collector.getError(i);
                if (message instanceof ExceptionMessage exceptionMessage) {
                    SecurityException found = GroovySandboxPolicy.findSecurityException(exceptionMessage.getCause());
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
