package com.tarzan.maxkb4j.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseUtil {

    public static DataSource getDataSource(String databaseType, String host, int port, String databaseName, String username, String password) {
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

    private static String buildJdbcUrl(String databaseType, String host, int port, String databaseName) {
        switch (databaseType.toLowerCase()) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, databaseName);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "oracle":
                return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, databaseName);
            case "mssql":
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;", host, port, databaseName);
            case "sqlite":
                return String.format("jdbc:sqlite:%s", databaseName); // SQLite 使用文件路径作为数据库名
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
    }
}
