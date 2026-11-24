package com.maxkb4j.core.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class DatabaseUtil {

    public static DataSource getDataSource(String databaseType, String host, Integer port, String username, String password, String databaseName) {
        String jdbcUrl = buildJdbcUrl(databaseType, host, port, databaseName);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        // 可选配置（根据需要调整）
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        return new HikariDataSource(config);
    }

    public static String getSqlDialect(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve database product name", e);
        }
    }

    public static String generateDDL(DataSource dataSource) {
        StringBuilder ddl = new StringBuilder();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            // 使用 try-with-resources 自动关闭 ResultSet
            try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String createTableStatement = generateCreateTableStatement(tableName, metaData);
                    ddl.append(createTableStatement).append("\n");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate DDL from DataSource", e);
        }
        return ddl.toString();
    }

    public static String executeSqlQuery(String sqlQuery, DataSource dataSource) {
        // 解析校验并规范化为单条只读 SELECT，执行规范化后的 SQL 而非原始输入，
        // 确保实际执行的内容与解析校验的内容严格一致，杜绝多语句注入
        String safeSql = toReadOnlySelectSql(sqlQuery);
        // 使用 try-with-resources 自动管理资源
        try (Connection connection = dataSource.getConnection()) {
            // 事务级只读：setReadOnly 在 autocommit 下对 PostgreSQL 不生效，
            // 必须显式开启事务，由 pgjdbc 发出 BEGIN READ ONLY，服务端拒绝一切写操作
            // （含数据修改型 CTE、SELECT INTO、DDL、FOR UPDATE）
            connection.setAutoCommit(false);
            try {
                connection.setReadOnly(true);
                try (Statement statement = connection.createStatement()) {
                    return execute(safeSql, statement);
                }
            } finally {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    log.warn("Failed to rollback read-only query transaction", e);
                }
                connection.setReadOnly(false);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Failed to execute SQL query", e);
            throw new RuntimeException("SQL query execution failed", e);
        }
    }

    /**
     * 校验 SQL 并规范化为单条只读 SELECT：
     * <ul>
     *     <li>仅允许单条语句：parse() 只解析第一条语句并静默丢弃其后内容，
     *         可被 "select id from users; drop table users" 这类多语句注入绕过，
     *         因此改用 parseStatements() 并要求恰好一条语句；</li>
     *     <li>拒绝 SELECT INTO：在 PostgreSQL / SQL Server 上会实际建表写数据，
     *         且 INTO 可能隐藏在 WITH 子查询（WithItem）或 UNION 分支内，需递归检查；</li>
     *     <li>返回由解析结果重新渲染的 SQL，丢弃注释及语句之外的任何附加内容。</li>
     * </ul>
     */
    protected static String toReadOnlySelectSql(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query must not be blank");
        }
        // 快速失败防线（大小写不敏感）：放行 SELECT / WITH / 括号包裹的查询，
        // 其余语句族（INSERT、UPDATE、EXPLAIN、VALUES 等）直接拒绝
        String upperSql = sqlQuery.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH") && !upperSql.startsWith("(")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sqlQuery);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        if (statements.size() != 1) {
            log.warn("Blocked multi-statement query attempt: {}", sqlQuery);
            throw new IllegalArgumentException("Only a single SELECT statement is allowed");
        }
        // jsqlparser 的 Statement 与 java.sql.Statement 同名，沿用全限定名避免冲突
        net.sf.jsqlparser.statement.Statement statement = statements.get(0);
        if (!(statement instanceof Select select)) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        if (containsSelectInto(select)) {
            log.warn("Blocked SELECT INTO attempt: {}", sqlQuery);
            throw new IllegalArgumentException("SELECT INTO is not allowed");
        }
        // 用解析结果重新渲染，保证执行内容不含任何未被解析的附加语句
        return statement.toString();
    }

    /**
     * 递归检查 SELECT 语法树是否带有 INTO 子句（含 WITH 子查询与 UNION 等集合操作分支）。
     */
    private static boolean containsSelectInto(Select select) {
        if (select == null) {
            return false;
        }
        List<WithItem> withItems = select.getWithItemsList();
        if (withItems != null) {
            for (WithItem withItem : withItems) {
                if (containsSelectInto(withItem)) {
                    return true;
                }
            }
        }
        if (select instanceof PlainSelect plainSelect) {
            List<Table> intoTables = plainSelect.getIntoTables();
            return intoTables != null && !intoTables.isEmpty();
        }
        if (select instanceof SetOperationList setOperationList) {
            for (Select child : setOperationList.getSelects()) {
                if (containsSelectInto(child)) {
                    return true;
                }
            }
        }
        if (select instanceof ParenthesedSelect parenthesedSelect) {
            return containsSelectInto(parenthesedSelect.getSelect());
        }
        return false;
    }

    private static String execute(String sqlQuery, Statement statement) throws SQLException {
        List<String> resultRows = new ArrayList<>();
        ResultSet resultSet = statement.executeQuery(sqlQuery);
        try {
            int columnCount = resultSet.getMetaData().getColumnCount();
            List<String> columnNames = new ArrayList<>();

            for (int i = 1; i <= columnCount; ++i) {
                columnNames.add(resultSet.getMetaData().getColumnName(i));
            }

            resultRows.add(String.join(",", columnNames));

            while (resultSet.next()) {
                List<String> columnValues = new ArrayList<>();
                for (int i = 1; i <= columnCount; ++i) {
                    String columnValue = resultSet.getObject(i) == null ? "" : resultSet.getObject(i).toString();
                    if (columnValue.contains(",")) {
                        columnValue = "\"" + columnValue + "\"";
                    }
                    columnValues.add(columnValue);
                }
                resultRows.add(String.join(",", columnValues));
            }
        } catch (Throwable var11) {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Throwable var10) {
                    var11.addSuppressed(var10);
                }
            }
            throw var11;
        }
        resultSet.close();
        return String.join("\n", resultRows);
    }

    public static String cleanSql(String sqlQuery) {
        // LLM 输出可能为空（如内容被过滤），null 透传，由 toReadOnlySelectSql 统一报错
        if (sqlQuery == null) {
            return null;
        }
        int start = 0;
        int end = sqlQuery.length();
        if (sqlQuery.contains("```sql")) {
            start = sqlQuery.indexOf("```sql") + 6;
            int closingIndex = sqlQuery.lastIndexOf("```");
            if (closingIndex > start) {
                end = closingIndex;
            }
        } else if (sqlQuery.contains("```")) {
            start = sqlQuery.indexOf("```") + 3;
            int closingIndex = sqlQuery.lastIndexOf("```");
            if (closingIndex > start) {
                end = closingIndex;
            }
        }
        return sqlQuery.substring(start, end).trim();
    }

    private static String generateCreateTableStatement(String tableName, DatabaseMetaData metaData) {
        StringBuilder createTableStatement = new StringBuilder();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null);
             ResultSet pk = metaData.getPrimaryKeys(null, null, tableName);
             ResultSet fks = metaData.getImportedKeys(null, null, tableName)) {
            String primaryKeyColumn = "";
            if (pk.next()) {
                primaryKeyColumn = pk.getString("COLUMN_NAME");
            }
            createTableStatement.append("TABLE ").append(tableName);
            try (ResultSet tableRemarks = metaData.getTables(null, null, tableName, null)) {
                if (tableRemarks.next()) {
                    String tableComment = tableRemarks.getString("REMARKS");
                    if (tableComment != null && !tableComment.isEmpty()) {
                        createTableStatement.append("(").append(tableComment).append(")");
                    }
                }
            }
            createTableStatement.append(" {\n");
            String columnName;
            String columnComment;
            while (columns.next()) {
                columnName = columns.getString("COLUMN_NAME");
                columnComment = columns.getString("TYPE_NAME");
                String comment = columns.getString("REMARKS");
                createTableStatement.append("  ").append(columnName).append(" ").append(columnComment);
                if (columnName.equals(primaryKeyColumn)) {
                    createTableStatement.append(" PRIMARY KEY");
                }
                if (comment != null && !comment.isEmpty()) {
                    createTableStatement.append("  COMMENT ").append(comment);
                }
                createTableStatement.append(",\n");
            }
            while (fks.next()) {
                columnName = fks.getString("FKCOLUMN_NAME");
                columnComment = fks.getString("PKTABLE_NAME");
                String pkColumnName = fks.getString("PKCOLUMN_NAME");
                createTableStatement.append("  FOREIGN KEY (").append(columnName).append(") REFERENCES ").append(columnComment).append("(").append(pkColumnName).append("),\n");
            }
            if (createTableStatement.charAt(createTableStatement.length() - 2) == ',') {
                createTableStatement.delete(createTableStatement.length() - 2, createTableStatement.length());
            }
            createTableStatement.append("\n}\n");
        } catch (SQLException var13) {
            throw new RuntimeException(var13);
        }

        return createTableStatement.toString();
    }

    /**
     * 主机名允许的字符：域名/IPv4/IPv6（方括号）与端口号冒号，
     * 禁止空格、引号及 URL 参数分隔符（? & # / ; =），防止注入 JDBC 连接参数。
     */
    private static final Pattern SAFE_HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.\\-]+(:\\d{1,5})?$|^\\[[0-9A-Fa-f:.]+\\](:\\d{1,5})?$");

    /**
     * 网络型数据库库名允许的字符（URL 路径段），禁止 ? & # / ; = % 等 URL 特殊字符。
     */
    private static final Pattern SAFE_DB_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.$\\-]+$");

    /**
     * SQLite 使用文件路径作为数据库名，允许路径分隔符，但禁止 URL 特殊字符。
     */
    private static final Pattern SAFE_SQLITE_PATH_PATTERN = Pattern.compile("^[A-Za-z0-9_.$\\-/:\\\\ ]+$");

    private static String buildJdbcUrl(String databaseType, String host, Integer port, String databaseName) {
        if (databaseType == null) {
            throw new IllegalArgumentException("Database type must not be null");
        }
        if (!"sqlite".equalsIgnoreCase(databaseType)) {
            if (host == null || !SAFE_HOST_PATTERN.matcher(host).matches()) {
                throw new IllegalArgumentException("Invalid database host: " + host);
            }
            if (port == null) {
                throw new IllegalArgumentException("Database port must not be null");
            }
        }
        return switch (databaseType.toLowerCase()) {
            case "mysql" -> {
                validateDbName(databaseName);
                yield String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, databaseName);
            }
            case "postgresql" -> {
                validateDbName(databaseName);
                yield String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            }
            case "oracle" -> {
                validateDbName(databaseName);
                yield String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, databaseName);
            }
            case "sqlserver" -> {
                // SQL Server 的连接串用分号分隔参数，额外禁止分号由 SAFE_DB_NAME_PATTERN 覆盖
                validateDbName(databaseName);
                yield String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;", host, port, databaseName);
            }
            case "sqlite" -> {
                // SQLite 使用文件路径作为数据库名
                if (databaseName == null || !SAFE_SQLITE_PATH_PATTERN.matcher(databaseName).matches()) {
                    throw new IllegalArgumentException("Invalid SQLite database path: " + databaseName);
                }
                yield String.format("jdbc:sqlite:%s", databaseName);
            }
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
    }

    private static void validateDbName(String databaseName) {
        if (databaseName == null || !SAFE_DB_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("Invalid database name: " + databaseName);
        }
    }
}

