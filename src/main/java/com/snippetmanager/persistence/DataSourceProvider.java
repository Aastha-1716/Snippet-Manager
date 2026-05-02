package com.snippetmanager.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Utility to create a HikariCP DataSource from environment variables.
 * Supports Railway MySQL, Neon, and other Postgres providers.
 */
public final class DataSourceProvider {
    private DataSourceProvider() {}

    public static DataSource createMySqlFromEnv() {
        String rawUrl = firstNonBlank(
                System.getenv("RAILWAY_MYSQL_URL"),
                System.getenv("MYSQL_URL"),
                System.getenv("MYSQL_PUBLIC_URL"),
                System.getenv("MYSQL_CONNECTION_URL"),
                System.getenv("DATABASE_URL")
        );
        String user = firstNonBlank(
                System.getenv("RAILWAY_MYSQL_USER"),
                System.getenv("MYSQLUSER"),
                System.getenv("MYSQL_USER"),
                System.getenv("DATABASE_USER")
        );
        String pass = firstNonBlank(
                System.getenv("RAILWAY_MYSQL_PASSWORD"),
                System.getenv("MYSQLPASSWORD"),
                System.getenv("MYSQL_PASSWORD"),
                System.getenv("DATABASE_PASSWORD")
        );

        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        ConnectionInfo info = parseMySqlUrl(rawUrl);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(info.jdbcUrl());
        if (info.username() != null && !info.username().isBlank()) {
            cfg.setUsername(info.username());
        } else if (user != null && !user.isBlank()) {
            cfg.setUsername(user);
        }
        if (info.password() != null && !info.password().isBlank()) {
            cfg.setPassword(info.password());
        } else if (pass != null && !pass.isBlank()) {
            cfg.setPassword(pass);
        }
        cfg.setMaximumPoolSize(6);
        cfg.addDataSourceProperty("useSSL", "true");
        cfg.addDataSourceProperty("requireSSL", "true");
        cfg.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        cfg.addDataSourceProperty("serverTimezone", "UTC");
        cfg.addDataSourceProperty("characterEncoding", "UTF-8");
        return new HikariDataSource(cfg);
    }

    public static DataSource createFromEnv() {
        String jdbc = firstNonBlank(
                System.getenv("NEON_JDBC_URL"),
                System.getenv("RAILWAY_DATABASE_URL"),
                System.getenv("DATABASE_URL"),
                System.getenv("POSTGRES_URL"),
                System.getenv("PGURL")
        );
        String user = firstNonBlank(
                System.getenv("NEON_DB_USER"),
                System.getenv("RAILWAY_DB_USER"),
                System.getenv("DATABASE_USER"),
                System.getenv("POSTGRES_USER"),
                System.getenv("PGUSER")
        );
        String pass = firstNonBlank(
                System.getenv("NEON_DB_PASSWORD"),
                System.getenv("RAILWAY_DB_PASSWORD"),
                System.getenv("DATABASE_PASSWORD"),
                System.getenv("POSTGRES_PASSWORD"),
                System.getenv("PGPASSWORD")
        );

        if (jdbc == null || jdbc.isBlank()) return null;

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(normalizeJdbcUrl(jdbc, "postgresql"));
        if (user != null && !user.isBlank()) cfg.setUsername(user);
        if (pass != null && !pass.isBlank()) cfg.setPassword(pass);
        cfg.setMaximumPoolSize(6);
        cfg.addDataSourceProperty("sslmode", "require");
        return new HikariDataSource(cfg);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String normalizeJdbcUrl(String rawUrl, String expectedScheme) {
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }
        if (!rawUrl.startsWith(expectedScheme + "://") && !rawUrl.startsWith(expectedScheme + ":")) {
            return rawUrl;
        }
        return "jdbc:" + rawUrl;
    }

    private static ConnectionInfo parseMySqlUrl(String rawUrl) {
        String normalized = rawUrl.startsWith("jdbc:") ? rawUrl.substring(5) : rawUrl;
        if (!normalized.startsWith("mysql://")) {
            return new ConnectionInfo(normalizeJdbcUrl(rawUrl, "mysql"), null, null);
        }

        URI uri = URI.create(normalized);
        StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://");
        if (uri.getHost() != null) {
            jdbcUrl.append(uri.getHost());
        }
        if (uri.getPort() > 0) {
            jdbcUrl.append(":").append(uri.getPort());
        }
        if (uri.getPath() != null && !uri.getPath().isBlank()) {
            jdbcUrl.append(uri.getPath());
        }
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            jdbcUrl.append("?useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        } else {
            jdbcUrl.append("?").append(query);
        }

        String username = null;
        String password = null;
        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            username = parts[0];
            if (parts.length > 1) {
                password = parts[1];
            }
        }

        return new ConnectionInfo(jdbcUrl.toString(), username, password);
    }

    private record ConnectionInfo(String jdbcUrl, String username, String password) {}
}
