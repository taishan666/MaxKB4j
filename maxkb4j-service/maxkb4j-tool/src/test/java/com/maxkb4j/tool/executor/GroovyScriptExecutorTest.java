package com.maxkb4j.tool.executor;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void execute_classNotInWhitelist_rejectedAtCompileTime() {
        // ClassExpression 被 SecureAST 编译期禁止，执行器会把编译失败还原为 SecurityException
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
}
