package com.ajayaventure.config;

import com.ajayaventure.util.EnvVars;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Application-wide JDBC connection pool backed by HikariCP.
 *
 * <p>Credentials come from environment configuration ({@link EnvVars}); never
 * from frontend code or committed files. Runtime user must be the least
 * privileged AV_APP schema account.</p>
 */
public final class Database {

    private static final AtomicReference<DataSource> INSTANCE = new AtomicReference<>();

    private Database() {
    }

    /**
     * Lazily builds the pool on first use so tests and health probes that never
     * touch the database do not require a live Oracle instance.
     */
    public static DataSource getDataSource() {
        DataSource current = INSTANCE.get();
        if (current != null) {
            return current;
        }
        synchronized (Database.class) {
            if (INSTANCE.get() != null) {
                return INSTANCE.get();
            }
            DataSource ds = build();
            INSTANCE.set(ds);
            return ds;
        }
    }

    /** Replaces the pool (used by integration tests). */
    public static void reset() {
        DataSource current = INSTANCE.getAndSet(null);
        if (current instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // best-effort shutdown
            }
        }
    }

    private static DataSource build() {
        String host = EnvVars.get("DB_HOST", "localhost");
        int port = EnvVars.getInt("DB_PORT", 1521);
        String service = EnvVars.get("DB_SERVICE", "orclpdb");
        String user = EnvVars.get("DB_USERNAME", "AV_APP");
        String password = EnvVars.get("DB_PASSWORD", "");

        String jdbcUrl = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(EnvVars.getInt("DB_POOL_MAX_SIZE", 10));
        config.setMinimumIdle(EnvVars.getInt("DB_POOL_MIN_IDLE", 2));
        config.setConnectionTimeout(EnvVars.getInt("DB_POOL_CONN_TIMEOUT_MS", 30000));
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        config.setPoolName("ajaya-pool");
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    /**
     * Direct connection helper for narrow cases (DDL migrations, admin tasks)
     * where going through the pool is undesirable. Callers must close it.
     */
    public static Connection openDirectConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /** Read-only facade used by health/readiness probes. */
    public static final class ReadOnly implements DataSource {
        private final DataSource delegate = Database.getDataSource();

        @Override
        public Connection getConnection() throws SQLException {
            Connection c = delegate.getConnection();
            c.setReadOnly(true);
            return c;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection c = delegate.getConnection(username, password);
            c.setReadOnly(true);
            return c;
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }
    }
}