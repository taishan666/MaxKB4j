package com.maxkb4j.tool.executor;

import com.maxkb4j.tool.sandbox.GroovySandboxInterceptor;
import cn.hutool.json.JSONUtil;
import com.maxkb4j.tool.sandbox.GroovySandboxPolicy;
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
}
