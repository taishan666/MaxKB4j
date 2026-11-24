package com.maxkb4j.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link DatabaseUtil#toReadOnlySelectSql(String)} 只读校验测试。
 * 覆盖两类历史绕过：多语句注入与 SELECT INTO（含 WITH / UNION 内嵌变体）。
 * 另覆盖 {@link DatabaseUtil#cleanSql(String)} 的围栏剥离与 null 透传。
 */
class DatabaseUtilTest {

    private static String normalize(String sql) {
        return DatabaseUtil.toReadOnlySelectSql(sql);
    }

    // ---- 应当放行 ----

    @Test
    void allowsPlainSelect() {
        assertEquals("SELECT id FROM users", normalize("select id from users"));
    }

    @Test
    void allowsCaseInsensitiveSelect() {
        assertEquals("SELECT id FROM users", normalize("SeLeCt id from users"));
    }

    @Test
    void allowsTrailingSemicolon() {
        assertEquals("SELECT id FROM users", normalize("select id from users;"));
    }

    @Test
    void allowsSemicolonInsideStringLiteral() {
        assertEquals("SELECT * FROM t WHERE a = 'x;y'", normalize("select * from t where a = 'x;y'"));
    }

    @Test
    void allowsCommentWithSemicolon() {
        assertEquals("SELECT 1", normalize("select 1 /*; drop table users*/"));
    }

    @Test
    void allowsWithQuery() {
        assertEquals("WITH c AS (SELECT 1) SELECT * FROM c", normalize("with c as (select 1) select * from c"));
    }

    @Test
    void allowsUnion() {
        assertEquals("SELECT * FROM t UNION SELECT * FROM t2", normalize("select * from t union select * from t2"));
    }

    @Test
    void allowsParenthesizedSelect() {
        assertEquals("(SELECT 1)", normalize("(select 1)"));
    }

    // ---- 应当拦截：多语句注入 ----

    @Test
    void blocksMultiStatementDrop() {
        assertThrows(IllegalArgumentException.class,
                () -> normalize("select id from users; drop table users"));
    }

    @Test
    void blocksMultiStatementUpdate() {
        assertThrows(IllegalArgumentException.class,
                () -> normalize("select 1; update users set id = 1"));
    }

    @Test
    void blocksEmptyStatementBetween() {
        assertThrows(IllegalArgumentException.class,
                () -> normalize("select 1; ; select 2"));
    }

    // ---- 应当拦截：SELECT INTO ----

    @Test
    void blocksSelectInto() {
        assertThrows(IllegalArgumentException.class,
                () -> normalize("SELECT * INTO t2 FROM t1"));
    }

    @Test
    void blocksSelectIntoInsideWithItem() {
        assertThrows(IllegalArgumentException.class,
                () -> normalize("WITH c AS (SELECT * INTO t2 FROM t1) SELECT * FROM c"));
    }

    @Test
    void blocksSelectIntoInsideUnionBranch() {
        // UNION 分支带 INTO（无论 jsqlparser 拒绝解析还是 AST 检查命中，都必须抛出）
        assertThrows(IllegalArgumentException.class,
                () -> normalize("SELECT id FROM t1 UNION SELECT * INTO t2 FROM t3"));
    }

    // ---- 应当拦截：非 SELECT 与解析失败（fail-closed） ----

    @Test
    void blocksDataModifyingCte() {
        // PostgreSQL 数据修改型 CTE：jsqlparser 解析失败，fail-closed 拦截
        assertThrows(IllegalArgumentException.class,
                () -> normalize("WITH d AS (DELETE FROM users RETURNING *) SELECT * FROM d"));
    }

    @Test
    void blocksNonSelectStatements() {
        assertThrows(IllegalArgumentException.class, () -> normalize("delete from users"));
        assertThrows(IllegalArgumentException.class, () -> normalize("update users set id = 1"));
        assertThrows(IllegalArgumentException.class, () -> normalize("insert into users values (1)"));
    }

    @Test
    void blocksNonSelectPrefixes() {
        // 与旧行为一致：VALUES / EXPLAIN 等非 SELECT 语句族不放开
        assertThrows(IllegalArgumentException.class, () -> normalize("values (1)"));
        assertThrows(IllegalArgumentException.class, () -> normalize("explain select 1"));
    }

    @Test
    void blocksUnparseableSql() {
        assertThrows(IllegalArgumentException.class, () -> normalize("select from where"));
    }

    @Test
    void blocksBlankSql() {
        assertThrows(IllegalArgumentException.class, () -> normalize("   "));
        assertThrows(IllegalArgumentException.class, () -> normalize(null));
    }

    // ---- cleanSql：围栏剥离与 null 透传 ----

    @Test
    void cleanSqlPassesThroughNull() {
        // LLM 返回空内容时不应 NPE，null 透传给 toReadOnlySelectSql 统一报错
        assertNull(DatabaseUtil.cleanSql(null));
    }

    @Test
    void cleanSqlStripsSqlFence() {
        assertEquals("select id from users",
                DatabaseUtil.cleanSql("```sql\nselect id from users\n```"));
    }

    @Test
    void cleanSqlStripsPlainFence() {
        assertEquals("select id from users",
                DatabaseUtil.cleanSql("```\nselect id from users\n```"));
    }

    @Test
    void cleanSqlTrimsPlainSql() {
        assertEquals("select id from users",
                DatabaseUtil.cleanSql("  select id from users  "));
    }
}
