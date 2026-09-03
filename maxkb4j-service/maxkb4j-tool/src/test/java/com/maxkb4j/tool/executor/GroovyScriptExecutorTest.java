package com.maxkb4j.tool.executor;

import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import com.maxkb4j.tool.sandbox.GroovySandboxPolicy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GroovyScriptExecutor 沙箱执行与编译缓存测试。
 */
class GroovyScriptExecutorTest {

    private static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void execute_simpleExpression_returnsResult() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor("a + b", null);
        assertEquals(3, executor.execute(params("a", 1, "b", 2)));
    }

    @Test
    void execute_filesReadAndWriteScript_allowed() throws Exception {
        // 文件操作白名单：java.nio.file.Files 静态方法与 Path.of 应通过编译期与运行期校验
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("maxkb4j-groovy-", ".txt");
        try {
            java.nio.file.Files.writeString(tempFile, "hello maxkb");
            String code = """
                    import java.nio.file.Files
                    import java.nio.file.Path

                    def p = Path.of("%s")
                    def content = Files.readString(p)
                    Files.writeString(p, content + "!")
                    return Files.readString(p) + "|" + Files.exists(p)
                    """.formatted(tempFile.toString().replace("\\", "\\\\"));
            GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
            assertEquals("hello maxkb!|true", executor.execute(params()));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void execute_filesScriptWithoutExplicitImport_allowed() throws Exception {
        // java.nio.file 已通过 ImportCustomizer 星号预导入，脚本无需显式 import
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("maxkb4j-groovy-", ".txt");
        try {
            java.nio.file.Files.writeString(tempFile, "preset import");
            String code = """
                    return Files.readString(Path.of("%s"))
                    """.formatted(tempFile.toString().replace("\\", "\\\\"));
            GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
            assertEquals("preset import", executor.execute(params()));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void execute_stringConcatenation_returnsResult() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor("name.toUpperCase() + '!'", null);
        assertEquals("MAXKB!", executor.execute(params("name", "maxkb")));
    }

    @Test
    void execute_sameScriptTwice_reusesCompiledClass() {
        String code = "value * 2";
        GroovyScriptExecutor first = new GroovyScriptExecutor(code, null);
        GroovyScriptExecutor second = new GroovyScriptExecutor(code, null);

        assertEquals(42, first.execute(params("value", 21)));
        assertTrue(GroovyScriptExecutor.isScriptCached(code), "首次执行后脚本应进入编译缓存");
        assertEquals(10, second.execute(params("value", 5)));
    }

    @Test
    void execute_dangerousToken_rejectedBeforeCompilation() {
        GroovyScriptExecutor executor =
                new GroovyScriptExecutor("Runtime.getRuntime().exec('ls')", null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    @Test
    void execute_dateTimeFormatterScript_returnsFormattedDateTime() {
        // 内置工具「获取当前时间」的脚本：DateTimeFormatter 类引用应能通过编译期与运行期白名单
        String code = """
                import java.time.LocalDateTime
                import java.time.format.DateTimeFormatter

                def now = LocalDateTime.now()
                def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                return now.format(formatter)
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params());
        assertTrue(result.toString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "应返回格式化后的日期时间字符串，实际: " + result);
    }

    @Test
    void execute_localDateTimePlusDays_allowed() {
        // 时间运算方法（plusDays 等）应通过运行期沙箱白名单
        String code = """
                import java.time.LocalDateTime
                def base = LocalDateTime.of(2026, 8, 26, 10, 30, 0)
                return base.plusDays(1).plusHours(2).withMinute(15).toString()
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("2026-08-27T12:15", executor.execute(params()));
    }

    @Test
    void execute_regexFindOperator_allowed() {
        // =~ 编译为 ScriptBytecodeAdapter.findRegex，应通过运行期白名单
        String code = """
                def m = "联系人: 张三 2026-08-26" =~ /(\\d{4})-(\\d{2})-(\\d{2})/
                if (m.find()) {
                    return m.group(0) + "|" + m.group(1)
                }
                return "no match"
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("2026-08-26|2026", executor.execute(params()));
    }

    @Test
    void execute_regexMatchOperator_allowed() {
        // ==~ 编译为 ScriptBytecodeAdapter.matchRegex，应通过运行期白名单
        String code = """
                return "abc123" ==~ /[a-z]+\\d+/
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals(true, executor.execute(params()));
    }

    @Test
    void execute_timestampConversionScript_timestampToDate() {
        // 内置工具「日期时间戳转换」：毫秒时间戳转 UTC+8 日期字符串
        String code = """
                import java.time.*
                import java.time.format.*

                final ZoneId UTC8 = ZoneId.of("Asia/Shanghai")
                final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                if (inputData instanceof Long) {
                    Instant instant = Instant.ofEpochMilli(inputData)
                    ZonedDateTime utc8Time = instant.atZone(UTC8)
                    return utc8Time.format(DATE_TIME_FORMATTER)
                }
                throw new IllegalArgumentException("不支持的输入")
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("2023-07-22 12:26:40", executor.execute(params("inputData", 1690000000000L)));
    }

    @Test
    void execute_timestampConversionScript_dateToTimestamp() {
        // 内置工具「日期时间戳转换」：UTC+8 日期字符串转毫秒时间戳
        String code = """
                import java.time.*
                import java.time.format.*

                final ZoneId UTC8 = ZoneId.of("Asia/Shanghai")
                final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                String str = inputData.trim()
                LocalDateTime localDateTime = LocalDateTime.parse(str, DATE_TIME_FORMATTER)
                ZonedDateTime utc8Zoned = localDateTime.atZone(UTC8)
                long timestampMillis = utc8Zoned.toInstant().toEpochMilli()
                return timestampMillis
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals(1690000000000L, executor.execute(params("inputData", "2023-07-22 12:26:40")));
    }

    @Test
    void execute_catchDateTimeParseException_allowed() {
        // 日期解析的常见写法：catch 具体异常类型 DateTimeParseException。
        // SecureASTCustomizer.visitVariableExpression 按精确类名校验变量静态类型，
        // 该异常类必须在编译期常量/变量类型白名单中，否则报
        // "Usage of variables of type [java.time.format.DateTimeParseException] is not allowed"
        String code = """
                import java.time.LocalDate
                import java.time.format.DateTimeFormatter
                import java.time.format.DateTimeParseException

                try {
                    return LocalDate.parse(inputData, DateTimeFormatter.ISO_LOCAL_DATE).toString()
                } catch (DateTimeParseException e) {
                    return "caught:" + e.getParsedString()
                }
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("caught:not-a-date", executor.execute(params("inputData", "not-a-date")));
    }

    @Test
    void execute_classNotInWhitelist_rejectedAtCompileTime() {
        // Thread 不在类引用白名单中，编译期即被拒绝，执行器把编译失败还原为 SecurityException
        GroovyScriptExecutor executor = new GroovyScriptExecutor("Thread.sleep(10)", null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    @Test
    void execute_nonWhitelistedConstructor_rejectedAtRuntime() {
        // Scanner 不在运行期白名单中：能通过编译期检查，由 GroovySandboxInterceptor 在运行期拦截
        GroovyScriptExecutor executor = new GroovyScriptExecutor("new java.util.Scanner('abc')", null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    @Test
    void execute_typedVariableDeclaration_returnsResult() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor("int x = 20; x + 1", null);
        assertEquals(21, executor.execute(params()));
    }

    @Test
    void execute_blankCode_returnsEmptyString() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor("   ", null);
        assertEquals("", executor.execute(params()));
    }

    @Test
    void execute_withInitParams_doesNotMutateCallerParams() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor("a + b", params("a", 100));
        Map<String, Object> callerParams = params("a", 1, "b", 2);

        assertEquals(102, executor.execute(callerParams));

        assertEquals(2, callerParams.size());
        assertEquals(1, callerParams.get("a"));
        assertEquals(2, callerParams.get("b"));
    }

    @Test
    void execute_withInitParams_mergeWithImmutableCallerParams() {
        // argumentsAsMap 对空参数返回不可变 Map.of()，合并不应对其产生写操作
        GroovyScriptExecutor executor = new GroovyScriptExecutor("a + b", params("a", 100));
        assertEquals(102, executor.execute(Map.of("a", 1, "b", 2)));
    }

    @Test
    void execute_withInitParams_initParamsOverrideCallerParams() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor("a + b", params("a", 100, "b", 200));
        assertEquals(300, executor.execute(params("a", 1, "b", 2)));
    }

    @Test
    void execute_enumConstantOnWhitelistedClass_allowed() {
        // 枚举常量通过类名读取等价于对 Class 对象的静态属性访问（Model.ONNX_PPOCR_V4），
        // 指向白名单类的类引用应放行，其它类仍被拒绝
        String code = """
                import io.github.mymonstercat.Model
                return Model.ONNX_PPOCR_V4
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals(io.github.mymonstercat.Model.ONNX_PPOCR_V4, executor.execute(params()));
    }

    @Test
    void execute_staticConstantOnWhitelistedClass_allowed() {
        String code = "return Integer.MAX_VALUE";
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals(Integer.MAX_VALUE, executor.execute(params()));
    }

    @Test
    void execute_springUtilGetBeanWithClassLiteral_passesSandbox() {
        // SpringUtil.getBean(IOssService.class)：白名单静态调用 + 指向白名单类的 Class 字面量参数均应放行。
        // 单测无 Spring 容器，getBean 内部抛 NPE 并被包装为 RuntimeException；
        // 只要不是 SecurityException，即说明沙箱未拦截该调用模式
        String code = """
                import com.maxkb4j.common.util.SpringUtil
                import com.maxkb4j.oss.service.IOssService
                return SpringUtil.getBean(IOssService.class)
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> executor.execute(params()));
        assertFalse(exception instanceof SecurityException, "沙箱不应拦截白名单类 Class 字面量参数: " + exception);
    }

    @Test
    void execute_catchExceptionWithPrintStackTrace_allowed() {
        String code = """
                try {
                    throw new IllegalArgumentException("boom")
                } catch (Exception e) {
                    e.printStackTrace()
                    return "caught:" + e.getMessage()
                }
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("caught:boom", executor.execute(params()));
    }

    @Test
    void execute_propertyReadOnPlatformDto_allowed() {
        // 绑定参数可能是平台数据类（如 imageList 中的 OssFile），属性读取应放行
        com.maxkb4j.common.domain.dto.OssFile ossFile = new com.maxkb4j.common.domain.dto.OssFile();
        ossFile.setFileId("fid-1");
        ossFile.setName("a.png");
        GroovyScriptExecutor executor = new GroovyScriptExecutor("file.fileId + '|' + file.name", null);
        assertEquals("fid-1|a.png", executor.execute(params("file", ossFile)));
    }

    @Test
    void execute_propertyWriteOnPlatformDto_rejected() {
        // 数据类只放开属性读取，写入仍按默认拒绝策略处理
        com.maxkb4j.common.domain.dto.OssFile ossFile = new com.maxkb4j.common.domain.dto.OssFile();
        ossFile.setName("a.png");
        GroovyScriptExecutor executor = new GroovyScriptExecutor("file.name = 'x'", null);
        assertThrows(SecurityException.class, () -> executor.execute(params("file", ossFile)));
    }

    @Test
    void execute_mathExpressionTool_allowed() {
        // 内置工具「数学公式执行」：exp4j 表达式求值应通过编译期变量类型白名单、
        // 表达式安全检查与运行期构造/接收者/方法白名单
        assertTrue(GroovySandboxPolicy.isAllowedClassName("net.objecthunter.exp4j.Expression"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("net.objecthunter.exp4j.ExpressionBuilder"));

        String code = """
                @Grab('net.objecthunter:exp4j:0.4.8')
                import net.objecthunter.exp4j.Expression;
                import net.objecthunter.exp4j.ExpressionBuilder;

                Expression engine = new ExpressionBuilder(expression)
                                .build();
                double result = engine.evaluate();
                return "result is " + result;
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("result is 8.0", executor.execute(params("expression", "(2+2)*2")));
    }

    @Test
    void execute_exp4jExpressionWithVariables_allowed() {
        // exp4j 变量声明与赋值（自定义数学工具的常见用法）同样应放行
        String code = """
                import net.objecthunter.exp4j.ExpressionBuilder

                def engine = new ExpressionBuilder("x + y * 2")
                        .variables("x", "y")
                        .build()
                        .setVariable("x", 1)
                        .setVariable("y", 3)
                return engine.evaluate()
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals(7.0d, executor.execute(params()));
    }

    @Test
    void interceptor_grapeGrabStaticCall_ignoredAsNoOp() throws Throwable {
        // @Grab 兜底：即使编译产物中残留 Grape.grab(...) 静态调用（转换禁用未生效的历史产物），
        // 运行期也按空操作忽略——不执行真实下载、不抛异常，脚本继续使用 classpath 依赖执行
        GroovySandboxInterceptor interceptor = new GroovySandboxInterceptor();
        assertNull(interceptor.onStaticCall(
                null, groovy.grape.Grape.class, "grab", Map.of("group", "net.objecthunter")));
    }

    @Test
    void sandboxPolicy_ocrRelatedWhitelistEntriesPresent() {
        // 以类名断言白名单条目（不加载 Class，避免本地环境差异），
        // 保障 OcrResult 属性读取、Model 枚举常量、InferenceEngine 静态调用等 OCR 场景
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.benjaminwan.ocrlibrary.OcrResult"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("io.github.mymonstercat.Model"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("io.github.mymonstercat.ocr.InferenceEngine"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.maxkb4j.common.domain.dto.OssFile")
                || GroovySandboxPolicy.isReadableDataClass(com.maxkb4j.common.domain.dto.OssFile.class));
    }
}
