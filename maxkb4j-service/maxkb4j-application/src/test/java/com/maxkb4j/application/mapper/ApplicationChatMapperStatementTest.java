package com.maxkb4j.application.mapper;

import com.maxkb4j.application.entity.ApplicationChatEntity;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * mapper XML 语句装配测试（无数据库）。
 * <p>
 * 本模块无嵌入式 PostgreSQL 测试环境，ON CONFLICT 等 PG 方言无法在测试库执行，
 * 此处只验证 XML 语句可被 MyBatis 正确解析、注册并与接口方法绑定
 * （含内联 typeHandler 的参数装配），避免问题拖到应用启动时才暴露。
 * </p>
 */
class ApplicationChatMapperStatementTest {

    private static final String STATEMENT_ID =
            "com.maxkb4j.application.mapper.ApplicationChatMapper.insertOrIncrementRecordCount";

    @Test
    void insertOrIncrementRecordCount_parsesAndBindsUpsertStatement() {
        // addMapper 会自动加载接口同包下的同名 XML（与生产 @MapperScan 的加载方式一致）
        Configuration configuration = new Configuration();
        configuration.addMapper(ApplicationChatMapper.class);

        MappedStatement statement = configuration.getMappedStatement(STATEMENT_ID);
        assertEquals(SqlCommandType.INSERT, statement.getSqlCommandType());

        ApplicationChatEntity entity = new ApplicationChatEntity();
        entity.setId("chat-1");
        entity.setSummary("s");
        entity.setApplicationId("app-1");
        entity.setChatUserId("user-1");
        entity.setChatUserType("1");
        entity.setIsDeleted(false);
        entity.setStarNum(0);
        entity.setTrampleNum(0);
        entity.setChatRecordCount(1);
        entity.setMarkSum(0);
        entity.setIpAddress("");
        BoundSql boundSql = statement.getBoundSql(entity);

        String sql = boundSql.getSql();
        assertTrue(sql.contains("ON CONFLICT (id) DO UPDATE SET"), "应包含主键冲突时的原子自增分支");
        assertTrue(sql.contains("chat_record_count = application_chat.chat_record_count + 1"),
                "冲突分支应原子累加 chat_record_count");

        // jsonb 参数（asker/meta/source）应装配为 JSONBTypeHandler
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        assertEquals(14, mappings.size(), "插入列数应与占位符数一致");
        long jsonbCount = mappings.stream()
                .filter(m -> m.getProperty().equals("asker") || m.getProperty().equals("meta") || m.getProperty().equals("source"))
                .filter(m -> m.getTypeHandler() instanceof com.maxkb4j.common.typehandler.JSONBTypeHandler)
                .count();
        assertEquals(3, jsonbCount, "asker/meta/source 三个 jsonb 列应绑定 JSONBTypeHandler");
    }
}
