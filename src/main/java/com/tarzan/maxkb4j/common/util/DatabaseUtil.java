package com.tarzan.maxkb4j.common.util;

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

@Slf4j
public class DatabaseUtil {

    public static DataSource getDataSource(String databaseType, String host, Integer port, String username, String password,String databaseName) {
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

    public static String executeSqlQuery(String sqlQuery,DataSource dataSource) {
        // 只允许 SELECT 查询
        if (!isSelect(sqlQuery)) {
            // 可选：记录警告日志
            return "";
        }
        // 验证 SQL（如防止注入等）
        validate(sqlQuery);
        // 使用 try-with-resources 自动管理资源
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            return execute(sqlQuery, statement);
        } catch (SQLException e) {
            log.error("Failed to execute SQL query", e);
        }
        return "";
    }

    protected static void validate(String sqlQuery) {
        if (!sqlQuery.startsWith("SELECT")){
            throw new IllegalArgumentException("SQL query must start");
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

            for(int i = 1; i <= columnCount; ++i) {
                columnNames.add(resultSet.getMetaData().getColumnName(i));
            }

            resultRows.add(String.join(",", columnNames));

            while(resultSet.next()) {
                List<String> columnValues = new ArrayList<>();
                for(int i = 1; i <= columnCount; ++i) {
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
        if (sqlQuery.contains("```sql")) {
            return sqlQuery.substring(sqlQuery.indexOf("```sql") + 6, sqlQuery.lastIndexOf("```"));
        } else {
            return sqlQuery.contains("```") ? sqlQuery.substring(sqlQuery.indexOf("```") + 3, sqlQuery.lastIndexOf("```")) : sqlQuery;
        }
    }

    private static String generateCreateTableStatement(String tableName, DatabaseMetaData metaData) {
        StringBuilder createTableStatement = new StringBuilder();
        try {
            ResultSet columns = metaData.getColumns(null, null, tableName, null);
            ResultSet pk = metaData.getPrimaryKeys(null, null, tableName);
            ResultSet fks = metaData.getImportedKeys(null, null, tableName);
            String primaryKeyColumn = "";
            if (pk.next()) {
                primaryKeyColumn = pk.getString("COLUMN_NAME");
            }
            createTableStatement.append("TABLE ").append(tableName);
            ResultSet tableRemarks = metaData.getTables(null, null, tableName, null);
            if (tableRemarks.next()) {
                String tableComment = tableRemarks.getString("REMARKS");
                if (tableComment != null && !tableComment.isEmpty()) {
                    createTableStatement.append("(").append(tableComment).append(")");
                }
            }
            createTableStatement.append(" {\n");
            String columnName;
            String columnComment;
            while(columns.next()) {
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
            while(fks.next()) {
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

    private static String buildJdbcUrl(String databaseType, String host, int port, String databaseName) {
        return switch (databaseType.toLowerCase()) {
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, databaseName);
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "oracle" -> String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, databaseName);
            case "sqlserver" -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;", host, port, databaseName);
            case "sqlite" -> String.format("jdbc:sqlite:%s", databaseName); // SQLite 使用文件路径作为数据库名
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
    }
}

