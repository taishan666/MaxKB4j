package com.maxkb4j.core.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;

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
        // 只允许 SELECT 查询，防止数据被修改
        if (!isSelect(sqlQuery)) {
            log.warn("Blocked non-SELECT query attempt: {}", sqlQuery);
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        // 验证 SQL（大小写不敏感）
        validate(sqlQuery);
        // 使用 try-with-resources 自动管理资源
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            connection.setReadOnly(true);
            return execute(sqlQuery, statement);
        } catch (SQLException e) {
            log.error("Failed to execute SQL query", e);
            throw new RuntimeException("SQL query execution failed", e);
        }
    }

    protected static void validate(String sqlQuery) {
        // 大小写不敏感，防止 select/Select 等小写变体绕过
        if (!sqlQuery.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
    }


    protected static boolean isSelect(String sqlQuery) {
        try {
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sqlQuery);
            return statement instanceof Select;
        } catch (JSQLParserException e) {
            return false;
        }
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

    private static String buildJdbcUrl(String databaseType, String host, int port, String databaseName) {
        if (databaseType == null) {
            throw new IllegalArgumentException("Database type must not be null");
        }
        if (host == null || !SAFE_HOST_PATTERN.matcher(host).matches()) {
            throw new IllegalArgumentException("Invalid database host: " + host);
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

