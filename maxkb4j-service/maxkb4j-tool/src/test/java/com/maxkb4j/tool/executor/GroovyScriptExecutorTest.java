package com.maxkb4j.tool.executor;

import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import cn.hutool.json.JSONUtil;
import com.maxkb4j.tool.sandbox.GroovySandboxPolicy;
import com.maxkb4j.tool.sandbox.GroovyScriptCache;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
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
    void execute_fastjsonJsonObjectAndArray_allowed() {
        // fastjson 用于内置工具 JSON 处理（如 web_search 结果解析）：
        // JSONObject/JSONArray 变量声明、JSON.parseObject 静态调用、类型化 getter 与构造器均应放行，
        // 否则编译期报 "Usage of variables of type [com.alibaba.fastjson.JSONArray] is not allowed"
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.alibaba.fastjson.JSON"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.alibaba.fastjson.JSONObject"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.alibaba.fastjson.JSONArray"));

        String code = """
                import com.alibaba.fastjson.JSON
                import com.alibaba.fastjson.JSONArray
                import com.alibaba.fastjson.JSONObject

                JSONObject user = JSON.parseObject(inputData)
                JSONArray tags = user.getJSONArray("tags")
                def joined = ""
                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) {
                        joined = joined + ","
                    }
                    joined = joined + tags.getString(i)
                }
                JSONObject result = new JSONObject()
                result.put("name", user.getString("name"))
                result.put("age", user.getInteger("age"))
                result.put("tags", joined.toString())
                return result.toJSONString()
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params("inputData",
                "{\"name\":\"maxkb\",\"age\":3,\"tags\":[\"rag\",\"kb\"]}"));
        com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(result.toString());
        assertEquals("maxkb", json.getString("name"));
        assertEquals(Integer.valueOf(3), json.getInteger("age"));
        assertEquals("rag,kb", json.getString("tags"));
    }

    @Test
    void execute_fastjsonJsonPath_rejectedAtCompileTime() {
        // fastjson 兼容包仅放开 JSON/JSONObject/JSONArray，JSONPath 等其它类仍在编译期拒绝
        String code = """
                import com.alibaba.fastjson.JSONPath
                return JSONPath.compile('$.name')
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    @Test
    void execute_scriptOwnStaticMethod_allowed() {
        // 脚本内定义并调用自身静态方法：sender 为脚本类（Script 子类），应放行
        String code = """
                public static String greet(String name) {
                    return "hello " + name
                }
                return greet(inputData)
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertEquals("hello maxkb", executor.execute(params("inputData", "maxkb")));
    }

    @Test
    void execute_fastjsonStaticMethodViaSubclass_allowed() {
        // JSONObject.parseArray / JSONObject.toJSONString 为继承自 JSON 的静态方法，
        // 经子类名调用时应沿继承链命中白名单
        String code = """
                import com.alibaba.fastjson.JSONArray
                import com.alibaba.fastjson.JSONObject

                JSONArray arr = JSONObject.parseArray(inputData)
                JSONObject obj = new JSONObject()
                obj.put("size", arr.size())
                return JSONObject.toJSONString(obj)
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params("inputData", "[1,2,3]"));
        assertEquals("{\"size\":3}", result.toString());
    }

    // ========== 调试面板字符串入参 vs AI 调用 List 入参 ==========

    /**
     * 复现线上报错：/tool/debug 的入参来自前端 el-input 文本框，无论字段声明为
     * string 还是 array，绑定进脚本的都是字符串（如 "[a,b,c]"）。
     * 脚本方法签名声明为 List 时，Groovy 不会把 String 隐式转成 List，
     * 于是抛 MissingMethodException（被 execute 包装成 RuntimeException）。
     */
    @Test
    void execute_listSignatureCalledWithStringParam_throwsMissingMethodException() {
        String code = """
                import java.util.List
                public static String render(List<?> xs, List<?> ys) {
                    return xs.size() + "/" + ys.size()
                }
                return render(xAxis, yAxis)
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> executor.execute(params("xAxis", "[a,b,c]", "yAxis", "[1,2,3]")));
        assertTrue(exception.getCause() instanceof groovy.lang.MissingMethodException,
                "根因应为方法签名不匹配: " + exception.getCause());
    }

    /**
     * 修复方式：脚本内先做入参归一化（toList），再调用 List 签名的业务方法。
     * 调试面板传的非严格 JSON 字符串（[a,b,c]）也能正确解析。
     */
    @Test
    void execute_echartsScript_withDebugStringParams_returnsEchartsHtml() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor(ECHARTS_TOOL_SCRIPT, null);
        Object result = executor.execute(params(
                "xAxis", "[a,b,c]",
                "yAxis", "[1,2,3]",
                "chartTitle", "折线图",
                "chartType", "line"));

        String html = String.valueOf(result);
        assertTrue(html.startsWith("<echarts_render>"), html);
        assertTrue(html.endsWith("</echarts_render>"), html);
        assertTrue(html.contains("\"text\":\"折线图\""), html);
        assertTrue(html.contains("\"type\":\"line\""), html);
        assertTrue(html.contains("\"data\":[\"a\",\"b\",\"c\"]"), html);
        assertTrue(html.contains("\"data\":[1,2,3]"), html);
        assertTrue(html.contains("\"trigger\":\"axis\""), html);
        assertTrue(html.contains("\"type\":\"max\""), html);
    }

    /** 调试面板传严格 JSON 数组字符串时，走 JSON.parseArray 分支。 */
    @Test
    void execute_echartsScript_withStrictJsonStringParams_returnsEchartsHtml() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor(ECHARTS_TOOL_SCRIPT, null);
        Object result = executor.execute(params(
                "xAxis", "[\"a\",\"b\",\"c\"]",
                "yAxis", "[1,2,3]",
                "chartTitle", "折线图",
                "chartType", "line"));

        String html = String.valueOf(result);
        assertTrue(html.contains("\"data\":[\"a\",\"b\",\"c\"]"), html);
        assertTrue(html.contains("\"data\":[1,2,3]"), html);
    }

    /** AI 调用路径：langchain4j 已把 arguments 解析成真实 List，脚本同样可用。 */
    @Test
    void execute_echartsScript_withListParams_returnsEchartsHtml() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor(ECHARTS_TOOL_SCRIPT, null);
        Object result = executor.execute(params(
                "xAxis", List.of("a", "b", "c"),
                "yAxis", List.of(1, 2, 3),
                "chartTitle", "折线图",
                "chartType", "line"));

        String html = String.valueOf(result);
        assertTrue(html.contains("\"data\":[\"a\",\"b\",\"c\"]"), html);
        assertTrue(html.contains("\"data\":[1,2,3]"), html);
    }

    /** 饼图分支：xAxis 作为 name、yAxis 作为 value 组装 [{value,name}]。 */
    @Test
    void execute_echartsScript_withPieType_buildsNameValuePairs() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor(ECHARTS_TOOL_SCRIPT, null);
        Object result = executor.execute(params(
                "xAxis", "[a,b,c]",
                "yAxis", "[1,2,3]",
                "chartTitle", "饼图",
                "chartType", "pie"));

        String html = String.valueOf(result);
        assertTrue(html.contains("\"type\":\"pie\""), html);
        assertTrue(html.contains("\"trigger\":\"item\""), html);
        assertTrue(html.contains("\"name\":\"a\""), html);
        assertTrue(html.contains("\"value\":1"), html);
        assertTrue(html.contains("\"value\":3"), html);
        assertFalse(html.contains("markPoint"), "饼图分支不应有 markPoint: " + html);
    }

    /**
     * ToolController#convertValue 会把 array 类型的调试入参转成 cn.hutool.json.JSONArray，
     * 它实现了 List/Collection，沙箱按集合类型放行，因此 List 签名的脚本方法无需改动即可使用。
     */
    @Test
    void execute_listSignature_withHutoolJsonArrayParam_shouldWork() {
        String code = """
                import java.util.List
                public static String render(List<?> xs, List<?> ys) {
                    return xs.size() + "/" + ys.size() + "/" + xs.get(0) + "/" + ys.get(0)
                }
                return render(xAxis, yAxis)
                """;
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params(
                "xAxis", JSONUtil.parseArray("[a,b,c]"),
                "yAxis", JSONUtil.parseArray("[1,2,3]")));
        assertEquals("3/3/a/1", result);
    }

    /**
     * ECharts 图表工具脚本（修复版）：
     * 业务方法保持 List 签名，入参在调用前经 toList 归一化，
     * 同时兼容调试面板字符串与 AI 调用的真实集合。
     */
    private static final String ECHARTS_TOOL_SCRIPT = """
            import com.alibaba.fastjson.JSON
            import com.alibaba.fastjson.JSONArray
            import com.alibaba.fastjson.JSONObject

            import java.util.List

            // 入参归一化：List/JSONArray 直接返回；字符串兼容 ["a","b"] 与 [a,b,c] 两种写法
            static List<Object> toList(Object value) {
                List<Object> result = new ArrayList<Object>()
                if (value == null) {
                    return result
                }
                if (value instanceof List) {
                    result.addAll((List) value)
                    return result
                }
                String text = value.toString().trim()
                if (text.isEmpty()) {
                    return result
                }
                String body = text
                if (body.length() > 1 && '['.equals(body.substring(0, 1))
                        && ']'.equals(body.substring(body.length() - 1))) {
                    body = body.substring(1, body.length() - 1)
                    try {
                        result.addAll(JSON.parseArray(text))
                        return result
                    } catch (Exception ignored) {
                        // 非严格 JSON（如 [a,b,c]），继续走下面的容错切分
                    }
                }
                if (body.trim().isEmpty()) {
                    return result
                }
                List<String> items = Arrays.asList(body.split(','))
                for (int i = 0; i < items.size(); i++) {
                    String item = items.get(i).trim()
                    if (item.length() > 1 && '"'.equals(item.substring(0, 1))
                            && '"'.equals(item.substring(item.length() - 1))) {
                        item = item.substring(1, item.length() - 1)
                    }
                    result.add(toValue(item))
                }
                return result
            }

            // 能转数字就转数字，ECharts 的 data 需要数值型
            static Object toValue(String text) {
                if (text == null || text.isEmpty()) {
                    return text
                }
                try {
                    return new BigDecimal(text)
                } catch (Exception ignored) {
                    return text
                }
            }

            public static String generateEChartsHtml(List<?> xAxisData, List<?> yAxisData, String chartTitle, String chartType) {
                JSONObject style = new JSONObject()
                style.put("height", "400px")
                style.put("width", "100%")

                JSONObject title = new JSONObject()
                title.put("text", chartTitle)
                title.put("left", "center")

                JSONObject option = new JSONObject()
                option.put("title", title)

                JSONObject series = new JSONObject()
                series.put("type", chartType)

                JSONObject tooltip = new JSONObject()
                if (!"pie".equals(chartType)) {
                    JSONObject xAxis = new JSONObject()
                    xAxis.put("type", "category")
                    xAxis.put("boundaryGap", false)
                    xAxis.put("data", xAxisData)

                    JSONObject yAxis = new JSONObject()
                    yAxis.put("type", "value")

                    JSONObject markPointDataMax = new JSONObject()
                    markPointDataMax.put("type", "max")
                    markPointDataMax.put("name", "最大值")

                    JSONObject markPointDataMin = new JSONObject()
                    markPointDataMin.put("type", "min")
                    markPointDataMin.put("name", "最小值")

                    JSONArray markPointDataArray = new JSONArray()
                    markPointDataArray.add(markPointDataMax)
                    markPointDataArray.add(markPointDataMin)

                    JSONObject onlineMarkPoint = new JSONObject()
                    onlineMarkPoint.put("data", markPointDataArray)

                    series.put("data", yAxisData)
                    series.put("markPoint", onlineMarkPoint)

                    option.put("xAxis", xAxis)
                    option.put("yAxis", yAxis)

                    tooltip.put("trigger", "axis")
                } else {
                    JSONArray seriesData = new JSONArray()
                    for (int i = 0; i < xAxisData.size(); i++) {
                        JSONObject dataItem = new JSONObject()
                        dataItem.put("value", yAxisData.get(i))
                        dataItem.put("name", xAxisData.get(i))
                        seriesData.add(dataItem)
                    }
                    series.put("data", seriesData)
                    tooltip.put("trigger", "item")
                }

                option.put("tooltip", tooltip)
                JSONArray seriesArray = new JSONArray()
                seriesArray.add(series)
                option.put("series", seriesArray)

                JSONObject formSetting = new JSONObject()
                formSetting.put("actionType", "JSON")
                formSetting.put("style", style)
                formSetting.put("option", option)

                return "<echarts_render>" + JSONObject.toJSONString(formSetting) + "</echarts_render>"
            }

            return generateEChartsHtml(toList(xAxis), toList(yAxis), chartTitle, chartType)
            """;

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

    @Test
    void sandboxPolicy_databaseQueryToolWhitelistEntriesPresent() {
        // 内置「MySQL 查询」工具依赖 groovy.sql.Sql 建连查询、JsonBuilder 序列化结果集，
        // 以及 Timestamp/Date/byte[] 字段类型转换，白名单需完整覆盖，否则编译期报
        // "Expression [ClassExpression] is not allowed: groovy.sql.Sql"
        assertTrue(GroovySandboxPolicy.isAllowedClassName("groovy.sql.Sql"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("groovy.json.JsonBuilder"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.sql.Timestamp"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.sql.Date"));
        assertTrue(GroovySandboxPolicy.isStaticCallAllowed("groovy.sql.Sql", "withInstance"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("rows"));
        assertTrue(GroovySandboxPolicy.isConstructorAllowed("groovy.json.JsonBuilder"));
        assertTrue(GroovySandboxPolicy.isConstructorAllowed("java.lang.String"));
    }

    @Test
    void execute_mysqlQueryResultSetProcessing_allowed() {
        // 复现内置「MySQL 查询」工具的结果集处理逻辑（不实际连库）：
        // collect/collectEntries 遍历、instanceof Timestamp/Date/BigDecimal/byte[] 类型分派、
        // new String(bytes,charset) 还原 BLOB、new JsonBuilder(...).toString() 序列化，
        // 均应通过编译期 ClassExpression 白名单（含 byte[] 数组解包）与运行期拦截器
        String code = """
                import groovy.json.JsonBuilder
                import java.sql.Timestamp
                import java.sql.Date
                import java.math.BigDecimal

                def processedRows = rows.collect { row ->
                    row.collectEntries { key, value ->
                        def processedValue = value
                        if (value instanceof Timestamp || value instanceof Date) {
                            processedValue = value.toInstant().toString()
                        } else if (value instanceof BigDecimal) {
                            processedValue = value.doubleValue()
                        } else if (value instanceof byte[]) {
                            processedValue = new String(value, 'UTF-8')
                        }
                        [(key): processedValue]
                    }
                }
                return new JsonBuilder(processedRows).toString()
                """;
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("ts", new java.sql.Timestamp(0L));
        row.put("n", new java.math.BigDecimal("1.5"));
        row.put("b", "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        row.put("s", "plain");
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params("rows", List.of(row)));
        String json = result.toString();
        assertTrue(json.contains("\"ts\":\"1970-01-01T00:00:00Z\""), json);
        assertTrue(json.contains("\"n\":1.5"), json);
        assertTrue(json.contains("\"b\":\"abc\""), json);
        assertTrue(json.contains("\"s\":\"plain\""), json);
    }

    @Test
    void sandboxPolicy_postgresqlQueryToolWhitelistEntriesPresent() {
        // PostgreSQL 查询类脚本白名单：Sql.newInstance 建连、close 释放连接、toDouble 转换
        // BigDecimal 为模板（sql.rows 方案）所需；eachRow/getMetaData/getColumnName/times/
        // leftShift 为用户自定义 eachRow 脚本所需（模板已改用 rows/collect/each，见下方编译测试）
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.sql.ResultSetMetaData"));
        assertTrue(GroovySandboxPolicy.isStaticCallAllowed("groovy.sql.Sql", "newInstance"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("eachRow"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("getMetaData"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("getColumnName"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("times"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("toDouble"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("close"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("leftShift"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("toMapString"));
    }

    @Test
    void sandboxPolicy_groovyRowResultAllowedAsMap() {
        // sql.rows(query) 返回 GroovyRowResult（implements Map），isAllowedType 需按
        // java.util.Map 接口白名单放行；DGM 扩展 toMapString(Map) 才能作用于它。
        // 对比 eachRow 的 GroovyResultSet 代理（非 Map）——toMapString 对其不适用
        assertTrue(GroovySandboxPolicy.isAllowedType(groovy.sql.GroovyRowResult.class));
        assertTrue(java.util.Map.class.isAssignableFrom(groovy.sql.GroovyRowResult.class));
        // GroovyResultSet 代理不是 Map，toMapString 无法作用（运行期会 MissingMethodException）
        assertFalse(java.util.Map.class.isAssignableFrom(groovy.sql.GroovyResultSet.class));
    }

    @Test
    void sandboxPolicy_eachRowJdkProxyReceiverAllowed() {
        // 运行期复现：Sql.eachRow 传给闭包的 row 是 GroovyResultSetProxy 创建的 JDK 动态代理
        // （类名形如 jdk.proxy2.$Proxy187），直接接口为 groovy.sql.GroovyResultSet。
        // isAllowedType 需按接口白名单放行，否则报"不允许在类 jdk.proxyN.$ProxyM 上调用方法: getMetaData"
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{groovy.sql.GroovyResultSet.class},
                (p, m, args) -> null);
        assertTrue(proxy.getClass().getName().contains("$Proxy"), proxy.getClass().getName());
        assertTrue(GroovySandboxPolicy.isAllowedType(proxy.getClass()));
    }

    @Test
    void execute_postgresqlQueryScript_compiles() {
        // 复现内置「PostgreSQL 查询」工具脚本（sql.rows 方案，仅验证编译期，不实际连库）：
        // Sql sql = null 变量类型声明、Sql.newInstance 受信静态调用（"newInstance" 在
        // 危险方法名黑名单中，需静态白名单优先放行）、rows/collect/each/putAt/toInstant/
        // toDouble/toString/close 均应通过 SecureASTCustomizer 与编译期表达式检查器。
        // 模板已从 eachRow 改为 sql.rows：eachRow 的 row 是 GroovyResultSetProxy 动态代理，
        // 运行期不支持 toMapString；sql.rows 返回 GroovyRowResult（本身就是 Map），更健壮
        String code = """
                import groovy.sql.Sql
                import groovy.json.JsonBuilder
                import java.sql.Timestamp
                import java.math.BigDecimal

                def url = "jdbc:postgresql://${host}:${port}/${database}"
                def driver = 'org.postgresql.Driver'

                Sql sql = null
                try {
                    sql = Sql.newInstance(url, user, password, driver)
                    println "连接成功！"

                    def result = sql.rows(query).collect { row ->
                        def map = [:]
                        row.each { columnName, value ->
                            if (value instanceof Timestamp) {
                                map[columnName] = value.toInstant().toString()
                            } else if (value instanceof BigDecimal) {
                                map[columnName] = value.toDouble()
                            } else {
                                map[columnName] = value
                            }
                        }
                        map
                    }

                    return new JsonBuilder(result).toString()
                } catch (Exception e) {
                    println "发生错误：${e.message}"
                    throw e
                } finally {
                    sql?.close()
                }
                """;
        // GroovyScriptCache.get 触发编译（不执行）：编译期任一层拦截
        // （变量类型/ClassExpression/危险方法名）都会抛 SecurityException 使本测试失败。
        // 注意：GroovyScriptExecutor 构造器并不编译，必须经缓存 get 才真正走编译期沙箱
        GroovyScriptCache.get(code);
        assertTrue(GroovyScriptExecutor.isScriptCached(code));
    }

    @Test
    void sandboxPolicy_mongoQueryToolWhitelistEntriesPresent() throws Exception {
        // 内置「MongoDB 查询」工具依赖 MongoClients.create 建连、getDatabase/getCollection
        // 定位集合、find().forEach 遍历、Document.parse 解析查询条件、ObjectId 转换 _id，
        // 白名单需完整覆盖，否则编译期报
        // "Usage of variables of type [com.mongodb.client.MongoClient] is not allowed"
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.mongodb.client.MongoClient"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.mongodb.client.MongoClients"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.mongodb.client.MongoDatabase"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.mongodb.client.MongoCollection"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("com.mongodb.client.FindIterable"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("org.bson.Document"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("org.bson.types.ObjectId"));
        assertTrue(GroovySandboxPolicy.isStaticCallAllowed("com.mongodb.client.MongoClients", "create"));
        assertTrue(GroovySandboxPolicy.isStaticCallAllowed("org.bson.Document", "parse"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("getDatabase"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("getCollection"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("forEach"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("find"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("close"));
        assertTrue(GroovySandboxPolicy.isConstructorAllowed("org.bson.Document"));
        assertTrue(GroovySandboxPolicy.isConstructorAllowed("org.bson.types.ObjectId"));
        // 运行期接收者是 internal 实现类（部分为包私有，需 Class.forName 加载），
        // isAllowedType 需沿接口链命中白名单
        assertTrue(GroovySandboxPolicy.isAllowedType(Class.forName("com.mongodb.client.internal.MongoClientImpl")));
        assertTrue(GroovySandboxPolicy.isAllowedType(Class.forName("com.mongodb.client.internal.MongoDatabaseImpl")));
        assertTrue(GroovySandboxPolicy.isAllowedType(Class.forName("com.mongodb.client.internal.MongoCollectionImpl")));
        assertTrue(GroovySandboxPolicy.isAllowedType(Class.forName("com.mongodb.client.internal.FindIterableImpl")));
    }

    @Test
    void execute_mongoDocumentResultProcessing_allowed() {
        // 复现「MongoDB 查询」工具的结果处理逻辑（离线执行，不实际连库）：
        // new Document(map) 构造、instanceof ObjectId 转换 _id、each 遍历中
        // instanceof byte[] 还原二进制字段、JsonOutput.toJson 序列化，
        // 均应通过编译期 ClassExpression/变量类型白名单与运行期拦截器
        String code = """
                import org.bson.Document
                import org.bson.types.ObjectId
                import groovy.json.JsonOutput

                Document doc = new Document(row)
                if (doc.get("_id") instanceof ObjectId) {
                    doc.put("_id", doc.get("_id").toString())
                }
                doc.each { key, value ->
                    if (value instanceof byte[]) {
                        doc.put(key, new String(value, "UTF-8"))
                    }
                }
                return JsonOutput.toJson(doc)
                """;
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("_id", new org.bson.types.ObjectId("507f1f77bcf86cd799439011"));
        row.put("name", "test");
        row.put("blob", "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params("row", row));
        String json = result.toString();
        assertTrue(json.contains("\"_id\":\"507f1f77bcf86cd799439011\""), json);
        assertTrue(json.contains("\"name\":\"test\""), json);
        assertTrue(json.contains("\"blob\":\"abc\""), json);
    }

    @Test
    void execute_mongoQueryScript_compiles() {
        // 复现内置「MongoDB 查询」工具完整脚本（仅验证编译期，不实际连库）：
        // MongoClient/MongoDatabase/MongoCollection/Document 变量类型声明、
        // MongoClients.create 受信静态调用、Document.parse 查询条件解析、
        // find().forEach 遍历、JsonBuilder/JsonOutput 序列化、finally 中 close，
        // 均应通过 SecureASTCustomizer 与编译期表达式检查器。
        // 注意：@Grab 在沙箱中被禁用（no-op），mongodb-driver-sync 由平台 classpath 提供；
        // System.err 属于危险类 java.lang.System，脚本内需使用 println 输出日志
        String code = """
                @Grab('org.mongodb:mongodb-driver-sync:4.11.1')
                import com.mongodb.client.MongoClients
                import com.mongodb.client.MongoClient
                import com.mongodb.client.MongoCollection
                import com.mongodb.client.MongoDatabase
                import org.bson.Document
                import org.bson.types.ObjectId
                import groovy.json.JsonBuilder
                import groovy.json.JsonOutput

                import java.time.format.DateTimeFormatter
                import java.time.Instant
                import java.time.ZoneId

                MongoClient client = null
                try {
                    String connectionString = "mongodb://${user}:${password}@${host}:${port}/?authSource=admin"
                    client = MongoClients.create(connectionString)

                    MongoDatabase db = client.getDatabase(database)
                    MongoCollection<Document> col = db.getCollection(collection)

                    Document queryDoc
                    if (query instanceof String) {
                        String qStr = query.trim()
                        if (!qStr) {
                            qStr = "{}"
                        }
                        queryDoc = Document.parse(qStr)
                    } else if (query instanceof Map) {
                        queryDoc = new Document(query)
                    } else {
                        throw new IllegalArgumentException("Query must be a JSON string or a Map")
                    }

                    def results = []
                    col.find(queryDoc).forEach { doc ->
                        if (doc.containsKey("_id") && doc.get("_id") instanceof ObjectId) {
                            doc.put("_id", doc.get("_id").toString())
                        }
                        doc.each { key, value ->
                            if (value instanceof byte[]) {
                                doc.put(key, new String(value, "UTF-8"))
                            }
                        }
                        results << doc
                    }

                    def serialize = { obj ->
                        if (obj == null) return null
                        if (obj instanceof Date) {
                            Instant instant = obj.toInstant()
                            return instant.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        }
                        if (obj instanceof byte[]) {
                            return new String(obj, "UTF-8")
                        }
                        return obj
                    }

                    def jsonBuilder = new JsonBuilder()
                    jsonBuilder.call(results.collect { doc ->
                        doc.collectEntries { k, v ->
                            [(k): serialize(v)]
                        }
                    })

                    return JsonOutput.prettyPrint(jsonBuilder.toString())
                } catch (Exception e) {
                    println("Error while connecting to MongoDB: ${e.message}")
                    e.printStackTrace()
                    throw e
                } finally {
                    if (client) {
                        client.close()
                    }
                }
                """;
        // GroovyScriptCache.get 触发编译（不执行）：编译期任一层拦截都会抛 SecurityException
        GroovyScriptCache.get(code);
        assertTrue(GroovyScriptExecutor.isScriptCached(code));
    }

    /**
     * 内置「MongoDB 查询」工具的 query 归一化逻辑（与模板脚本保持一致，不连库）。
     * 大模型常把 query 填成 mongo shell 语句，直接交给 Document.parse 会抛
     * "JSON reader was expecting a value but found 'db'"。
     */
    private static final String MONGO_QUERY_NORMALIZE_CODE = """
            import org.bson.Document
            import groovy.json.JsonOutput

            def extractJsonBlocks = { String text ->
                def blocks = []
                int depth = 0
                int start = -1
                boolean inString = false
                boolean escaped = false
                int len = text.length()
                for (int i = 0; i < len; i++) {
                    String ch = text.substring(i, i + 1)
                    if (inString) {
                        if (escaped) {
                            escaped = false
                        } else if ('\\\\'.equals(ch)) {
                            escaped = true
                        } else if ('"'.equals(ch)) {
                            inString = false
                        }
                        continue
                    }
                    if ('"'.equals(ch)) {
                        inString = true
                    } else if ('{'.equals(ch) || '['.equals(ch)) {
                        if (depth == 0) {
                            start = i
                        }
                        depth = depth + 1
                    } else if ('}'.equals(ch) || ']'.equals(ch)) {
                        depth = depth - 1
                        if (depth == 0 && start >= 0) {
                            def block = new LinkedHashMap()
                            block.put("start", start)
                            block.put("json", text.substring(start, i + 1))
                            blocks << block
                            start = -1
                        }
                    }
                }
                return blocks
            }

            def blockAfter = { String text, List blocks, String keyword ->
                int idx = text.indexOf(keyword)
                if (idx < 0) {
                    return null
                }
                for (int i = 0; i < blocks.size(); i++) {
                    def block = blocks.get(i)
                    if (block.get("start") > idx) {
                        return block.get("json").toString()
                    }
                }
                return null
            }

            def numberAfter = { String text, String keyword ->
                int idx = text.indexOf(keyword)
                if (idx < 0) {
                    return null
                }
                String tail = text.substring(idx + keyword.length())
                String digits = ""
                int tailLen = tail.length()
                for (int i = 0; i < tailLen; i++) {
                    String ch = tail.substring(i, i + 1)
                    if ('0123456789'.contains(ch)) {
                        digits = "${digits}${ch}"
                    } else {
                        break
                    }
                }
                if (digits.isEmpty()) {
                    return null
                }
                return Integer.valueOf(digits)
            }

            Document queryDoc
            Document projectionDoc = null
            Document sortDoc = null
            Integer limitNum = null
            Integer skipNum = null

            if (query instanceof Map) {
                queryDoc = new Document(query)
            } else {
                String qStr = query == null ? "" : query.toString().trim()
                if (qStr.isEmpty()) {
                    qStr = "{}"
                }
                if (qStr.startsWith("{")) {
                    queryDoc = Document.parse(qStr)
                } else if (qStr.startsWith("[")) {
                    throw new IllegalArgumentException('query 不支持聚合管道写法')
                } else {
                    def blocks = extractJsonBlocks(qStr)
                    if (blocks.size() == 0) {
                        throw new IllegalArgumentException('query 必须是 MongoDB JSON 过滤条件')
                    }
                    String filterJson = blockAfter(qStr, blocks, "find(")
                    if (filterJson == null) {
                        filterJson = blockAfter(qStr, blocks, "findOne(")
                    }
                    if (filterJson == null) {
                        filterJson = blocks.get(0).get("json").toString()
                    }
                    if (filterJson.startsWith("[")) {
                        throw new IllegalArgumentException('query 不支持聚合管道写法')
                    }
                    queryDoc = Document.parse(filterJson)

                    int filterIdx = qStr.indexOf(filterJson)
                    int filterEnd = filterIdx + filterJson.length()
                    for (int i = 0; i < blocks.size(); i++) {
                        def block = blocks.get(i)
                        def blockStart = block.get("start")
                        if (blockStart >= filterEnd) {
                            String between = qStr.substring(filterEnd, blockStart)
                            if (!between.contains(")")) {
                                projectionDoc = Document.parse(block.get("json").toString())
                            }
                            break
                        }
                    }

                    String sortJson = blockAfter(qStr, blocks, ".sort(")
                    if (sortJson != null) {
                        sortDoc = Document.parse(sortJson)
                    }
                    limitNum = numberAfter(qStr, ".limit(")
                    skipNum = numberAfter(qStr, ".skip(")
                }
            }

            def out = new LinkedHashMap()
            out.put("filter", queryDoc.toJson())
            out.put("projection", projectionDoc == null ? null : projectionDoc.toJson())
            out.put("sort", sortDoc == null ? null : sortDoc.toJson())
            out.put("limit", limitNum)
            out.put("skip", skipNum)
            return JsonOutput.toJson(out)
            """;

    @Test
    void execute_mongoQueryNormalization_parsesShellStyleQuery() {
        // db.user.find({"name":"zhangsan"}, {"_id":0}).sort({"age":-1}).limit(10).skip(5)
        GroovyScriptExecutor executor = new GroovyScriptExecutor(MONGO_QUERY_NORMALIZE_CODE, null);
        Object result = executor.execute(params("query",
                "db.user.find({\"name\":\"zhangsan\"}, {\"_id\":0}).sort({\"age\":-1}).limit(10).skip(5)"));
        String json = result.toString();
        assertTrue(json.contains("zhangsan"), json);
        assertFalse(json.contains("\"projection\":null"), json);
        assertFalse(json.contains("\"sort\":null"), json);
        assertTrue(json.contains("\"limit\":10"), json);
        assertTrue(json.contains("\"skip\":5"), json);
    }

    @Test
    void execute_mongoQueryNormalization_keepsPlainJsonQuery() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor(MONGO_QUERY_NORMALIZE_CODE, null);
        Object result = executor.execute(params("query", "{\"age\":{\"$gt\":18}}"));
        String json = result.toString();
        assertTrue(json.contains("$gt"), json);
        assertTrue(json.contains("\"projection\":null"), json);
        assertTrue(json.contains("\"limit\":null"), json);
    }

    @Test
    void execute_mongoQueryNormalization_rejectsUnparsableQuery() {
        GroovyScriptExecutor executor = new GroovyScriptExecutor(MONGO_QUERY_NORMALIZE_CODE, null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> executor.execute(params("query", "db")));
        // 测试环境无 Spring MessageSource，外层 message 只有 i18n key，脚本原始提示在 cause 上
        String cause = String.valueOf(ex.getCause().getMessage());
        assertTrue(cause.contains("query 必须是 MongoDB JSON 过滤条件"), cause);
    }

    @Test
    void execute_startsWith_notRejectedByDangerousTokenScan() {
        // 回归：脚本文本预检的危险标记若写成裸 ".start"，会误杀白名单方法 startsWith
        GroovyScriptExecutor executor = new GroovyScriptExecutor("""
                return query.startsWith("{") ? "json" : "shell"
                """, null);
        assertEquals("json", executor.execute(params("query", "{\"a\":1}")));
        assertEquals("shell", executor.execute(params("query", "db.a.find({})")));
    }

    @Test
    void mongoToolTemplateScript_passesTokenScanAndCompiles() throws Exception {
        // 校验内置「MongoDB 数据库查询」工具模板：JSON 转义正确、脚本可通过文本预检与沙箱编译期检查
        java.nio.file.Path template = java.nio.file.Path.of("..", "..", "maxkb4j-start", "src", "main",
                "resources", "templates", "tool", "database_search", "MongoDB+数据库查询-1.0.0.tool");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(template),
                "模板文件不存在，跳过：" + template.toAbsolutePath());
        String script = JSONUtil.parseObj(java.nio.file.Files.readString(template,
                java.nio.charset.StandardCharsets.UTF_8)).getStr("code");
        assertNull(GroovySandboxPolicy.findDangerousToken(script));
        GroovyScriptCache.get(script);
        assertTrue(GroovyScriptExecutor.isScriptCached(script));
    }

    /**
     * 白名单必须覆盖内置「邮箱消息推送」工具脚本用到的类/方法/构造器。
     */
    @Test
    void sandboxPolicy_emailPushToolWhitelistEntriesPresent() {
        assertTrue(GroovySandboxPolicy.isAllowedClassName(
                "org.springframework.mail.javamail.JavaMailSenderImpl"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName(
                "org.springframework.mail.SimpleMailMessage"));
        // props.put(...) 的运行期接收者类型（getJavaMailProperties 返回值）
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.util.Properties"));
        assertTrue(GroovySandboxPolicy.isConstructorAllowed(
                "org.springframework.mail.javamail.JavaMailSenderImpl"));
        assertTrue(GroovySandboxPolicy.isConstructorAllowed(
                "org.springframework.mail.SimpleMailMessage"));
        for (String method : new String[]{"setHost", "setPort", "setUsername", "setPassword",
                "setDefaultEncoding", "setProtocol", "getJavaMailProperties", "setJavaMailProperties",
                "setFrom", "setTo", "setSubject", "setText", "send"}) {
            assertTrue(GroovySandboxPolicy.isMethodAllowed(method), "方法未在白名单中: " + method);
        }
    }

    /**
     * 回归测试：内置「邮箱消息推送」脚本的字符串字面量 "mail.smtp.starttls.enable" 中的
     * ".starttls" 曾因裸 token ".start" 的纯子串匹配被文本预检误判为危险调用
     * （报 "脚本包含不允许的危险调用：.start"）。token 已改为带左括号的 ".start("：
     * 字面量不再命中，真实的线程启动调用仍被拦截。
     */
    @Test
    void findDangerousToken_smtpStarttlsProperty_notRejected() {
        assertNull(GroovySandboxPolicy.findDangerousToken(
                "props.put(\"mail.smtp.starttls.enable\", tlsEnable)"));
        assertEquals(".start(", GroovySandboxPolicy.findDangerousToken(
                "new Thread(r).start()"));
    }

    /**
     * 内置「邮箱消息推送」工具模板脚本必须通过文本预检与编译期沙箱校验
     * （SecureASTCustomizer 变量类型白名单含 JavaMailSenderImpl/SimpleMailMessage/Properties）。
     * 仅编译不执行：执行会真实连接 SMTP 服务器发送邮件。
     */
    @Test
    void emailToolTemplateScript_passesTokenScanAndCompiles() throws Exception {
        java.nio.file.Path template = java.nio.file.Path.of("..", "..", "maxkb4j-start", "src", "main", "resources",
                "templates", "tool", "send_message", "邮箱消息推送-1.0.0.tool");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(template),
                "模板文件不存在，跳过：" + template.toAbsolutePath());
        String script = JSONUtil.parseObj(java.nio.file.Files.readString(template,
                java.nio.charset.StandardCharsets.UTF_8)).getStr("code");
        assertNull(GroovySandboxPolicy.findDangerousToken(script));
        GroovyScriptCache.get(script);
        assertTrue(GroovyScriptExecutor.isScriptCached(script));
    }

    // ==================== 受控 HTTP 客户端（脚本内发起 http/https 请求） ====================

    /**
     * 用户原始脚本（new URL + HttpURLConnection POST + JsonBuilder 请求体 + 读取响应）：
     * 针对本地 HTTP 服务器验证完整链路——构造 URL、openConnection、设置请求方法/头、
     * 写请求体（outputStream.withWriter）、读响应（inputStream.text）、disconnect 全部放行。
     */
    @Test
    void execute_httpPostScript_allowed() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        final String[] receivedMethod = {null};
        final String[] receivedAuth = {null};
        final String[] receivedContentType = {null};
        final String[] receivedBody = {null};
        server.createContext("/api/v1/search", exchange -> {
            receivedMethod[0] = exchange.getRequestMethod();
            receivedAuth[0] = exchange.getRequestHeaders().getFirst("Authorization");
            receivedContentType[0] = exchange.getRequestHeaders().getFirst("Content-Type");
            receivedBody[0] = new String(exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            byte[] resp = "{\"ok\":true,\"echo\":\"received\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        try {
            String code = """
                    import groovy.json.JsonBuilder
                    import groovy.json.JsonSlurper

                    def url = new URL("http://127.0.0.1:%d/api/v1/search")
                    def connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Authorization", "Bearer ${apiKey}")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Content-Type", "application/json")

                    def payload = new JsonBuilder([
                        q: query,
                        scope: "webpage",
                        includeSummary: false,
                        size: "10",
                        includeRawContent: false,
                        conciseSnippet: false
                    ]).toString()

                    connection.outputStream.withWriter { writer ->
                        writer << payload
                    }

                    def responseText = connection.inputStream.text
                    connection.disconnect()

                    return responseText
                    """.formatted(port);
            GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
            Object result = executor.execute(params("apiKey", "test-key-123", "query", "hello world"));
            assertEquals("{\"ok\":true,\"echo\":\"received\"}", result.toString());
            assertEquals("POST", receivedMethod[0]);
            assertEquals("Bearer test-key-123", receivedAuth[0]);
            assertEquals("application/json", receivedContentType[0]);
            assertTrue(receivedBody[0].contains("\"q\":\"hello world\""), receivedBody[0]);
            assertTrue(receivedBody[0].contains("\"scope\":\"webpage\""), receivedBody[0]);
        } finally {
            server.stop(0);
        }
    }

    /**
     * 用户提供的原始脚本（指向 https://metaso.cn）：仅验证编译期放行（文本扫描 + SecureASTCustomizer），
     * 不发起真实网络请求。证明 new URL / as HttpURLConnection / setRequestProperty / withWriter 等
     * 在编译期不被拒绝。
     */
    @Test
    void execute_userMetasoScript_compiles() {
        String code = """
                import groovy.json.JsonBuilder
                import groovy.json.JsonSlurper

                def url = new URL("https://metaso.cn/api/v1/search")
                def connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer ${apiKey}")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json")

                def payload = new JsonBuilder([
                    q: query,
                    scope: "webpage",
                    includeSummary: false,
                    size: "10",
                    includeRawContent: false,
                    conciseSnippet: false
                ]).toString()

                connection.outputStream.withWriter { writer ->
                    writer << payload
                }

                def responseText = connection.inputStream.text
                connection.disconnect()

                return responseText
                """;
        GroovyScriptCache.get(code);
        assertTrue(GroovyScriptExecutor.isScriptCached(code));
    }

    /** new URL("file:///...") 被协议白名单拒绝：防止借 URL 读取本地文件（LFI）。 */
    @Test
    void execute_urlWithFileProtocol_rejected() {
        String code = "return new URL('file:///etc/passwd').text";
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    /** new URL("jar:...") 等非 http/https 协议同样被拒绝。 */
    @Test
    void execute_urlWithJarProtocol_rejected() {
        String code = "return new URL('jar:file:///tmp/a.jar!/b').text";
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    /** Socket 仍被拦截：受控 HTTP 客户端只放行 URL/连接/流，不放开原始套接字。 */
    @Test
    void execute_socketConstruction_rejected() {
        String code = "return new Socket('127.0.0.1', 80)";
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        assertThrows(SecurityException.class, () -> executor.execute(params()));
    }

    /** 受控 HTTP 客户端白名单条目齐全：类名/构造器/方法/类型指派均放行，Socket/File 仍拦截。 */
    @Test
    void sandboxPolicy_httpClientWhitelistEntriesPresent() {
        // 类名白名单（编译期 ClassExpression 校验依赖）
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.net.URL"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.net.HttpURLConnection"));
        assertTrue(GroovySandboxPolicy.isAllowedClassName("java.net.URLConnection"));
        // 构造器白名单
        assertTrue(GroovySandboxPolicy.isConstructorAllowed("java.net.URL"));
        // 方法白名单
        assertTrue(GroovySandboxPolicy.isMethodAllowed("openConnection"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("setRequestProperty"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("disconnect"));
        assertTrue(GroovySandboxPolicy.isMethodAllowed("withWriter"));
        // 类型指派（运行期接收者校验依赖，含基类）
        assertTrue(GroovySandboxPolicy.isAllowedType(java.net.URL.class));
        assertTrue(GroovySandboxPolicy.isAllowedType(java.net.HttpURLConnection.class));
        assertTrue(GroovySandboxPolicy.isAllowedType(java.io.InputStream.class));
        assertTrue(GroovySandboxPolicy.isAllowedType(java.io.OutputStream.class));
        assertTrue(GroovySandboxPolicy.isAllowedType(java.io.Writer.class));
        // URL/连接/流不再被判定为危险类
        assertFalse(GroovySandboxPolicy.isDangerousClass(java.net.URL.class));
        assertFalse(GroovySandboxPolicy.isDangerousClass(java.net.HttpURLConnection.class));
        assertFalse(GroovySandboxPolicy.isDangerousClass(java.io.InputStream.class));
        // 但 Socket / File 仍是危险类，且不在类型白名单内
        assertTrue(GroovySandboxPolicy.isDangerousClass(java.net.Socket.class));
        assertTrue(GroovySandboxPolicy.isDangerousClass(java.io.File.class));
        assertFalse(GroovySandboxPolicy.isAllowedType(java.net.Socket.class));
        assertFalse(GroovySandboxPolicy.isAllowedType(java.io.File.class));
    }

    /** URL 协议校验：http/https 放行，file/jar/ftp 及无法识别的协议拒绝。 */
    @Test
    void validateUrlConstruction_protocolWhitelist() {
        // http/https 放行（不抛异常）
        GroovySandboxPolicy.validateUrlConstruction("https://metaso.cn/api/v1/search");
        GroovySandboxPolicy.validateUrlConstruction("http://127.0.0.1:8080/x");
        GroovySandboxPolicy.validateUrlConstruction("https", "metaso.cn", "/api/v1/search");
        // file/jar/ftp 拒绝
        assertThrows(SecurityException.class,
                () -> GroovySandboxPolicy.validateUrlConstruction("file:///etc/passwd"));
        assertThrows(SecurityException.class,
                () -> GroovySandboxPolicy.validateUrlConstruction("jar:file:///tmp/a.jar!/b"));
        assertThrows(SecurityException.class,
                () -> GroovySandboxPolicy.validateUrlConstruction("ftp://host/x"));
        // 无协议 / 无参数拒绝
        assertThrows(SecurityException.class,
                () -> GroovySandboxPolicy.validateUrlConstruction("metaso.cn/api"));
        assertThrows(SecurityException.class,
                () -> GroovySandboxPolicy.validateUrlConstruction());
    }

    // ==================== langchain4j Web Search（web_search 工具族） ====================

    /**
     * 内置「SearXNG 联网搜索」模板脚本（templates/tool/web_search/SearXNG-1.0.0.tool）必须通过
     * 文本预检与沙箱编译期校验：SearXNGWebSearchEngine 裸类名（经编译配置器星号导入解析）、
     * WebSearchResults/JSONObject 变量类型声明、SearXNGWebSearchEngine.builder() 静态工厂、
     * Map.of/Duration.ofSeconds 静态调用、builder 链式配置均应放行。
     * 仅编译不执行：执行会真实联网调用 SearXNG 服务。
     */
    @Test
    void searxngToolTemplateScript_passesTokenScanAndCompiles() throws Exception {
        java.nio.file.Path template = java.nio.file.Path.of("..", "..", "maxkb4j-start", "src", "main",
                "resources", "templates", "tool", "web_search", "SearXNG-1.0.0.tool");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(template),
                "模板文件不存在，跳过：" + template.toAbsolutePath());
        String script = JSONUtil.parseObj(java.nio.file.Files.readString(template,
                java.nio.charset.StandardCharsets.UTF_8)).getStr("code");
        assertNull(GroovySandboxPolicy.findDangerousToken(script));
        GroovyScriptCache.get(script);
        assertTrue(GroovyScriptExecutor.isScriptCached(script));
    }

    /**
     * 模板脚本的结果处理链路（离线执行，不联网）：
     * results.results().stream().map(e->{...}).toList() 中 WebSearchResults 接收者、
     * Stream.map、WebSearchOrganicResult 访问器（title/url/snippet/content/metadata）
     * 与 fastjson JSONObject 组装均应放行，最终返回 List&lt;JSONObject&gt;。
     */
    @Test
    void execute_webSearchResultsMapping_allowed() {
        String code = """
                import com.alibaba.fastjson.JSONObject

                return results.results().stream().map(e->{
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("title",e.title());
                    jsonObject.put("url",e.url());
                    jsonObject.put("snippet",e.snippet());
                    jsonObject.put("content",e.content());
                    jsonObject.put("metadata",e.metadata());
                    return jsonObject;
                }).toList();
                """;
        dev.langchain4j.web.search.WebSearchOrganicResult organic =
                new dev.langchain4j.web.search.WebSearchOrganicResult(
                        "MaxKB4j", java.net.URI.create("https://example.com/maxkb4j"),
                        "open source kb", "MaxKB4j content", Map.of("source", "unit-test"));
        dev.langchain4j.web.search.WebSearchResults webResults =
                new dev.langchain4j.web.search.WebSearchResults(
                        new dev.langchain4j.web.search.WebSearchInformationResult(1L), List.of(organic));
        GroovyScriptExecutor executor = new GroovyScriptExecutor(code, null);
        Object result = executor.execute(params("results", webResults));
        assertTrue(result instanceof List<?> list && list.size() == 1, "应返回单元素列表: " + result);
        com.alibaba.fastjson.JSONObject item = (com.alibaba.fastjson.JSONObject) ((List<?>) result).get(0);
        assertEquals("MaxKB4j", item.getString("title"));
        assertEquals("https://example.com/maxkb4j", item.getString("url"));
        assertEquals("open source kb", item.getString("snippet"));
        assertEquals("MaxKB4j content", item.getString("content"));
        assertEquals("unit-test", item.getJSONObject("metadata").getString("source"));
    }

    /**
     * 用户原始脚本（GoogleCustomWebSearchEngine.builder() 构建引擎 + search + 结果流转 JSONObject）：
     * 仅验证编译期放行（文本扫描 + SecureASTCustomizer 变量类型/ClassExpression 白名单），
     * 不发起真实网络请求。引擎构建参数 apiKey/csi/includeImages/timeout/maxRetries 均为绑定变量。
     */
    @Test
    void execute_googleCustomSearchScript_compiles() {
        String code = """
                import dev.langchain4j.web.search.WebSearchResults;
                import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
                import com.alibaba.fastjson.JSONObject;

                import java.time.Duration;
                import java.util.List;


                GoogleCustomWebSearchEngine searchEngine = GoogleCustomWebSearchEngine.builder()
                                .apiKey(apiKey)
                                .csi(csi)
                                .includeImages(includeImages)
                                .timeout(Duration.ofSeconds(timeout))
                                .maxRetries(maxRetries)
                                .build();
                WebSearchResults webSearchResults = searchEngine.search(query);
                return webSearchResults.results().stream().map(e->{
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("title",e.title());
                            jsonObject.put("url",e.url());
                            jsonObject.put("snippet",e.snippet());
                            jsonObject.put("content",e.content());
                            jsonObject.put("metadata",e.metadata());
                            return jsonObject;
                        }).toList();
                """;
        GroovyScriptCache.get(code);
        assertTrue(GroovyScriptExecutor.isScriptCached(code));
    }

    /**
     * 用户原始脚本（TavilyWebSearchEngine.builder() 构建引擎 + search + 结果流转 JSONObject）：
     * 仅验证编译期放行（文本扫描 + SecureASTCustomizer 变量类型/ClassExpression 白名单），
     * 不发起真实网络请求。引擎构建参数 apiKey/timeout 均为绑定变量。
     */
    @Test
    void execute_tavilySearchScript_compiles() {
        String code = """
                import dev.langchain4j.web.search.WebSearchResults;
                import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
                import com.alibaba.fastjson.JSONObject;

                import java.time.Duration;
                import java.util.List;


                TavilyWebSearchEngine searchEngine = TavilyWebSearchEngine.builder()
                        .apiKey(apiKey)
                        .timeout(Duration.ofSeconds(timeout))
                        .build();
                WebSearchResults webSearchResults = searchEngine.search(query);
                return webSearchResults.results().stream().map(e->{
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("title",e.title());
                    jsonObject.put("url",e.url());
                    jsonObject.put("snippet",e.snippet());
                    jsonObject.put("content",e.content());
                    jsonObject.put("metadata",e.metadata());
                    return jsonObject;
                }).toList();
                """;
        GroovyScriptCache.get(code);
        assertTrue(GroovyScriptExecutor.isScriptCached(code));
    }

    /**
     * 用户原始脚本（SearchApiWebSearchEngine.builder() + optionalParameters 自定义参数 + 结果流转 JSONObject）：
     * 仅验证编译期放行（文本扫描 + SecureASTCustomizer 变量类型/ClassExpression 白名单），
     * 不发起真实网络请求。optionalParameters 为 SearchApi 引擎特有 Builder 方法。
     */
    @Test
    void execute_searchApiSearchScript_compiles() {
        String code = """
                import dev.langchain4j.web.search.WebSearchResults;
                import dev.langchain4j.web.search.searchapi.SearchApiWebSearchEngine;
                import com.alibaba.fastjson.JSONObject;

                import java.time.Duration;
                import java.util.HashMap;
                import java.util.List;
                import java.util.Map;

                Map<String, Object> optionalParameters = new HashMap<>();
                optionalParameters.put("gl", "us");
                optionalParameters.put("hl", "en");
                optionalParameters.put("google_domain", "google.com");
                SearchApiWebSearchEngine searchEngine = SearchApiWebSearchEngine.builder()
                        .apiKey(apiKey)
                        .engine("google")
                        .optionalParameters(optionalParameters)
                        .timeout(Duration.ofSeconds(timeout))
                        .build();
                WebSearchResults webSearchResults = searchEngine.search(query);
                return webSearchResults.results().stream().map(e->{
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("title",e.title());
                    jsonObject.put("url",e.url());
                    jsonObject.put("snippet",e.snippet());
                    jsonObject.put("content",e.content());
                    jsonObject.put("metadata",e.metadata());
                    return jsonObject;
                }).toList();
                """;
        assertNull(GroovySandboxPolicy.findDangerousToken(code));
        GroovyScriptCache.get(code);
        assertTrue(GroovyScriptExecutor.isScriptCached(code));
    }
}
