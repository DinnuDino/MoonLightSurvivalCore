/*
 * Decompiled with CFR 0.152.
 */
package com.moonlight.vanilla.database;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class DatabaseManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;

    public DatabaseManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread thread = new Thread(r, "Moonlight-DB-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void initialize() throws SQLException {
        String dbType = this.plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        HikariConfig config = new HikariConfig();
        if ("MYSQL".equals(dbType)) {
            String host = this.plugin.getConfig().getString("database.mysql.host", "127.0.0.1");
            int port = this.plugin.getConfig().getInt("database.mysql.port", 3306);
            String db = this.plugin.getConfig().getString("database.mysql.database", "moonlight");
            String user = this.plugin.getConfig().getString("database.mysql.username", "moonlight_user");
            String pass = this.plugin.getConfig().getString("database.mysql.password", "");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&characterEncoding=utf8");
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(this.plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(this.plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 3));
        } else {
            File dbFile = new File(this.plugin.getDataFolder(), this.plugin.getConfig().getString("database.sqlite.filename", "moonlight_core.db"));
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;");
            config.setMaximumPoolSize(1);
        }
        config.setPoolName("MoonlightPool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(config);
        this.createTables();
    }

    private void createTables() throws SQLException {
        try (Connection conn = this.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS moonlight_homes (home_id VARCHAR(64) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, home_name VARCHAR(32) NOT NULL, world_name VARCHAR(64) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS moonlight_audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, event_type VARCHAR(32) NOT NULL, actor_uuid VARCHAR(36), target_uuid VARCHAR(36), details TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP);")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS player_bans (id INTEGER PRIMARY KEY AUTOINCREMENT, ban_id VARCHAR(32) UNIQUE NOT NULL, target_uuid VARCHAR(36) NOT NULL, target_name VARCHAR(16) NOT NULL, banned_by_uuid VARCHAR(36) NOT NULL, banned_by_name VARCHAR(32) NOT NULL, reason TEXT NOT NULL, issued_at TIMESTAMP NOT NULL, expires_at TIMESTAMP, duration_string VARCHAR(32) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT 1, unbanned_at TIMESTAMP, unbanned_by VARCHAR(32), unban_reason TEXT, ip_address VARCHAR(45), correlation_id VARCHAR(64));")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS player_mutes (id INTEGER PRIMARY KEY AUTOINCREMENT, target_uuid VARCHAR(36), target_name VARCHAR(16), muted_by VARCHAR(32), expires_at BIGINT);")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS moonlight_world_locations (player_uuid VARCHAR(36) NOT NULL, world_name VARCHAR(64) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (player_uuid, world_name));")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS moonlight_player_vaults (player_uuid VARCHAR(36) NOT NULL, vault_number INTEGER NOT NULL, inventory_data MEDIUMTEXT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (player_uuid, vault_number));")) {
                ps.executeUpdate();
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null || this.dataSource.isClosed()) {
            throw new SQLException("DataSource is closed or not initialized");
        }
        return this.dataSource.getConnection();
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, this.asyncExecutor).exceptionally(ex -> {
            this.plugin.getLogger().log(Level.SEVERE, "Database async operation failed!", (Throwable)ex);
            return null;
        });
    }

    public void shutdown() {
        if (this.asyncExecutor != null) {
            this.asyncExecutor.shutdown();
        }
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }
}

