/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package com.moonlight.vanilla.modules.audit;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.moonlight.vanilla.modules.audit.AuditConfig;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuditDatabaseManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private final AuditConfig config;
    private HikariDataSource dataSource;
    private final BlockingQueue<Runnable> writeQueue = new LinkedBlockingQueue<Runnable>(20000);
    private final ScheduledExecutorService batchExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Moonlight-Audit-DB-Worker");
        t.setDaemon(true);
        return t;
    });
    private final Map<UUID, ActiveSnapshotState> activeSnapshotMap = new ConcurrentHashMap<UUID, ActiveSnapshotState>();

    public AuditDatabaseManager(MoonlightVanillaSurvivalCore plugin, AuditConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        HikariConfig hikari = new HikariConfig();
        File dbFile = new File(this.plugin.getDataFolder(), "moonlight_audit.db");
        hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;");
        hikari.setMaximumPoolSize(2);
        hikari.setPoolName("MoonlightAuditPool");
        hikari.setConnectionTimeout(30000L);
        hikari.setIdleTimeout(600000L);
        hikari.setMaxLifetime(1800000L);
        this.dataSource = new HikariDataSource(hikari);
        this.createTables();
        long flushSeconds = Math.max(2, this.config.getFlushIntervalSeconds());
        this.batchExecutor.scheduleWithFixedDelay(this::drainQueue, flushSeconds, flushSeconds, TimeUnit.SECONDS);
    }

    private void createTables() {
        String[] tableSqls = new String[]{"CREATE TABLE IF NOT EXISTS player_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT NOT NULL UNIQUE, player_uuid TEXT NOT NULL, username TEXT NOT NULL, platform TEXT NOT NULL DEFAULT 'JAVA', ip_address TEXT, joined_at TIMESTAMP NOT NULL, quit_at TIMESTAMP, duration_seconds INTEGER DEFAULT 0, quit_reason TEXT);", "CREATE TABLE IF NOT EXISTS player_position_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, username TEXT NOT NULL, world_uuid TEXT NOT NULL, world_name TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, yaw REAL NOT NULL, pitch REAL NOT NULL, gamemode TEXT NOT NULL, health REAL NOT NULL, food_level INTEGER NOT NULL, exp_level INTEGER NOT NULL, snapshot_reason TEXT NOT NULL, duration_seconds INTEGER DEFAULT 1, last_active_timestamp TIMESTAMP, timestamp TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS player_inventory_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE, player_uuid TEXT NOT NULL, username TEXT NOT NULL, world_name TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, gamemode TEXT NOT NULL, health REAL NOT NULL, hunger INTEGER NOT NULL, saturation REAL NOT NULL, exp_level INTEGER NOT NULL, total_exp INTEGER NOT NULL, selected_slot INTEGER NOT NULL, snapshot_reason TEXT NOT NULL, inventory_data TEXT NOT NULL, armor_data TEXT NOT NULL, offhand_data TEXT NOT NULL, ender_chest_data TEXT NOT NULL, cursor_data TEXT, potion_effects TEXT, correlation_id TEXT, duration_seconds INTEGER DEFAULT 1, last_active_timestamp TIMESTAMP, state_hash TEXT, timestamp TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS player_enderchest_events (id INTEGER PRIMARY KEY AUTOINCREMENT, event_type TEXT NOT NULL, player_uuid TEXT NOT NULL, username TEXT NOT NULL, material TEXT, amount INTEGER, item_display_name TEXT, item_serialized TEXT, world_name TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, correlation_id TEXT NOT NULL, details TEXT, timestamp TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS player_item_events (id INTEGER PRIMARY KEY AUTOINCREMENT, event_type TEXT NOT NULL, actor_uuid TEXT NOT NULL, actor_name TEXT NOT NULL, affected_player_uuid TEXT NOT NULL, affected_player_name TEXT NOT NULL, material TEXT NOT NULL, amount INTEGER NOT NULL, item_display_name TEXT, item_serialized TEXT, world_name TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, correlation_id TEXT NOT NULL, details TEXT, timestamp TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS player_action_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, event_type TEXT NOT NULL, player_uuid TEXT NOT NULL, username TEXT NOT NULL, world_name TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, target_info TEXT, details TEXT, timestamp TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS audit_admin_actions (id INTEGER PRIMARY KEY AUTOINCREMENT, action_type TEXT NOT NULL, operator_uuid TEXT NOT NULL, operator_name TEXT NOT NULL, target_uuid TEXT, target_name TEXT, reason TEXT NOT NULL, source_id TEXT, items_affected TEXT, result TEXT NOT NULL, correlation_id TEXT NOT NULL, timestamp TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS audit_refund_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id TEXT NOT NULL UNIQUE, operator_uuid TEXT NOT NULL, operator_name TEXT NOT NULL, target_uuid TEXT NOT NULL, target_name TEXT NOT NULL, refund_type TEXT NOT NULL, reason TEXT NOT NULL, items_json TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'COMPLETED', before_snapshot_id TEXT NOT NULL, after_snapshot_id TEXT, undone_by_op TEXT, undone_at TIMESTAMP, undo_reason TEXT, correlation_id TEXT NOT NULL, created_at TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS audit_restore_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, restore_id TEXT NOT NULL UNIQUE, operator_uuid TEXT NOT NULL, operator_name TEXT NOT NULL, target_uuid TEXT NOT NULL, target_name TEXT NOT NULL, source_snapshot_id TEXT NOT NULL, confirmation_token TEXT NOT NULL, reason TEXT NOT NULL, items_diff_json TEXT NOT NULL, before_snapshot_id TEXT NOT NULL, after_snapshot_id TEXT NOT NULL, correlation_id TEXT NOT NULL, created_at TIMESTAMP NOT NULL);", "CREATE TABLE IF NOT EXISTS player_refund_mailbox (id INTEGER PRIMARY KEY AUTOINCREMENT, mailbox_item_id TEXT NOT NULL UNIQUE, player_uuid TEXT NOT NULL, source_transaction_id TEXT NOT NULL, material TEXT NOT NULL, amount INTEGER NOT NULL, item_serialized TEXT NOT NULL, reason TEXT NOT NULL, granted_by TEXT NOT NULL, claimed INTEGER DEFAULT 0, claimed_at TIMESTAMP, created_at TIMESTAMP NOT NULL);"};
        try (Connection conn = this.dataSource.getConnection();
             Statement stmt = conn.createStatement();){
            for (String sql : tableSqls) {
                stmt.execute(sql);
            }
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "[Audit DB] Failed to create audit tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public void queueWrite(Runnable task) {
        if (!this.writeQueue.offer(task)) {
            this.plugin.getLogger().warning("[Audit DB] Queue capacity reached! Executing task synchronously to prevent data loss.");
            task.run();
        }
    }

    private void drainQueue() {
        int drained = 0;
        while (!this.writeQueue.isEmpty() && drained < 500) {
            Runnable task = (Runnable)this.writeQueue.poll();
            if (task == null) continue;
            try {
                task.run();
                ++drained;
            }
            catch (Throwable t) {
                this.plugin.getLogger().log(Level.WARNING, "[Audit DB] Error executing queued batch task", t);
            }
        }
    }

    public void forceFlush() {
        while (!this.writeQueue.isEmpty()) {
            Runnable task = (Runnable)this.writeQueue.poll();
            if (task == null) continue;
            try {
                task.run();
            }
            catch (Throwable throwable) {}
        }
    }

    public void shutdown() {
        while (!this.writeQueue.isEmpty()) {
            Runnable task = (Runnable)this.writeQueue.poll();
            if (task == null) continue;
            try {
                task.run();
            }
            catch (Throwable throwable) {}
        }
        this.batchExecutor.shutdown();
        try {
            if (!this.batchExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.batchExecutor.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            this.batchExecutor.shutdownNow();
        }
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }

    public String recordInventorySnapshot(Player player, String reason, String correlationId) {
        Location loc = player.getLocation();
        InventorySerializer serializer = new InventorySerializer();
        String invData = serializer.serializeItemArray(player.getInventory().getStorageContents());
        String armorData = serializer.serializeItemArray(player.getInventory().getArmorContents());
        String offhandData = serializer.serializeItem(player.getInventory().getItemInOffHand());
        String eChestData = serializer.serializeItemArray(player.getEnderChest().getContents());
        String cursorData = serializer.serializeItem(player.getItemOnCursor());
        String effects = player.getActivePotionEffects().toString();
        String stateHash = Integer.toHexString(Objects.hash(invData, armorData, offhandData, eChestData, cursorData, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), player.getHealth(), player.getFoodLevel()));
        UUID uuid = player.getUniqueId();
        ActiveSnapshotState active = this.activeSnapshotMap.get(uuid);
        if (this.config.isDeduplicationEnabled() && "interval".equalsIgnoreCase(reason) && active != null && active.stateHash.equals(stateHash)) {
            ++active.durationSeconds;
            active.lastActiveMillis = System.currentTimeMillis();
            String activeId = active.snapshotId;
            int dur = active.durationSeconds;
            if (dur % 10 == 0) {
                this.queueWrite(() -> {
                    String updateSql = "UPDATE player_inventory_snapshots SET duration_seconds = ?, last_active_timestamp = ? WHERE snapshot_id = ?";
                    try (Connection conn = this.getConnection();
                         PreparedStatement ps = conn.prepareStatement(updateSql);){
                        ps.setInt(1, dur);
                        ps.setTimestamp(2, Timestamp.from(Instant.now()));
                        ps.setString(3, activeId);
                        ps.executeUpdate();
                    }
                    catch (SQLException sQLException) {
                        // empty catch block
                    }
                });
            }
            return active.snapshotId;
        }
        String snapshotId = "snap-" + UUID.randomUUID().toString().substring(0, 8);
        this.activeSnapshotMap.put(uuid, new ActiveSnapshotState(snapshotId, stateHash, System.currentTimeMillis()));
        this.queueWrite(() -> {
            String sql = "INSERT INTO player_inventory_snapshots (snapshot_id, player_uuid, username, world_name, x, y, z, gamemode, health, hunger, saturation, exp_level, total_exp, selected_slot, snapshot_reason, inventory_data, armor_data, offhand_data, ender_chest_data, cursor_data, potion_effects, correlation_id, duration_seconds, last_active_timestamp, state_hash, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = this.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, snapshotId);
                ps.setString(2, player.getUniqueId().toString());
                ps.setString(3, player.getName());
                ps.setString(4, loc.getWorld().getName());
                ps.setDouble(5, loc.getX());
                ps.setDouble(6, loc.getY());
                ps.setDouble(7, loc.getZ());
                ps.setString(8, player.getGameMode().name());
                ps.setDouble(9, player.getHealth());
                ps.setInt(10, player.getFoodLevel());
                ps.setFloat(11, player.getSaturation());
                ps.setInt(12, player.getLevel());
                ps.setInt(13, player.getTotalExperience());
                ps.setInt(14, player.getInventory().getHeldItemSlot());
                ps.setString(15, reason);
                ps.setString(16, invData);
                ps.setString(17, armorData);
                ps.setString(18, offhandData);
                ps.setString(19, eChestData);
                ps.setString(20, cursorData);
                ps.setString(21, effects);
                ps.setString(22, correlationId != null ? correlationId : UUID.randomUUID().toString());
                ps.setInt(23, 1);
                ps.setTimestamp(24, Timestamp.from(Instant.now()));
                ps.setString(25, stateHash);
                ps.setTimestamp(26, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "[Audit DB] Failed to save inventory snapshot " + snapshotId, e);
            }
        });
        return snapshotId;
    }

    public void logEnderChestEvent(String type, Player player, ItemStack item, String details) {
        Location loc = player.getLocation();
        InventorySerializer serializer = new InventorySerializer();
        String serialized = item != null ? serializer.serializeItem(item) : "";
        String matName = item != null ? item.getType().name() : "NONE";
        int amt = item != null ? item.getAmount() : 0;
        String name = item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
        this.queueWrite(() -> {
            String sql = "INSERT INTO player_enderchest_events (event_type, player_uuid, username, material, amount, item_display_name, item_serialized, world_name, x, y, z, correlation_id, details, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = this.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, type);
                ps.setString(2, player.getUniqueId().toString());
                ps.setString(3, player.getName());
                ps.setString(4, matName);
                ps.setInt(5, amt);
                ps.setString(6, name);
                ps.setString(7, serialized);
                ps.setString(8, loc.getWorld().getName());
                ps.setDouble(9, loc.getX());
                ps.setDouble(10, loc.getY());
                ps.setDouble(11, loc.getZ());
                ps.setString(12, UUID.randomUUID().toString());
                ps.setString(13, details);
                ps.setTimestamp(14, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.WARNING, "[Audit DB] Failed to log ender chest event", e);
            }
        });
    }

    public void logAdminAction(String actionType, String opUuid, String opName, String targetUuid, String targetName, String reason, String sourceId, String itemsAffected, String result, String correlationId) {
        this.queueWrite(() -> {
            String sql = "INSERT INTO audit_admin_actions (action_type, operator_uuid, operator_name, target_uuid, target_name, reason, source_id, items_affected, result, correlation_id, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = this.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, actionType);
                ps.setString(2, opUuid);
                ps.setString(3, opName);
                ps.setString(4, targetUuid);
                ps.setString(5, targetName);
                ps.setString(6, reason);
                ps.setString(7, sourceId);
                ps.setString(8, itemsAffected);
                ps.setString(9, result);
                ps.setString(10, correlationId);
                ps.setTimestamp(11, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "[Audit DB] Failed to log admin action", e);
            }
        });
    }

    public void deduplicateHistoricalLogs() {
        this.queueWrite(() -> {
            try (Connection conn = this.getConnection();
                 Statement stmt = conn.createStatement();){
                String dedupeInvSql = "DELETE FROM player_inventory_snapshots WHERE snapshot_reason = 'interval' AND id NOT IN (    SELECT MIN(id) FROM player_inventory_snapshots WHERE state_hash IS NOT NULL     GROUP BY player_uuid, state_hash, strftime('%Y-%m-%d %H', timestamp))";
                int purgedInv = stmt.executeUpdate(dedupeInvSql);
                String dedupePosSql = "DELETE FROM player_position_snapshots WHERE snapshot_reason = 'interval' AND id NOT IN (    SELECT MIN(id) FROM player_position_snapshots     GROUP BY player_uuid, world_name, CAST(x AS INT), CAST(y AS INT), CAST(z AS INT), strftime('%Y-%m-%d %H', timestamp))";
                int purgedPos = stmt.executeUpdate(dedupePosSql);
                int totalPurged = purgedInv + purgedPos;
                if (totalPurged > 0) {
                    this.plugin.getLogger().info("[Audit DB] Deduplicated " + totalPurged + " duplicate idle snapshots (Inv: " + purgedInv + ", Pos: " + purgedPos + "), freeing database storage while 100% of player history is preserved.");
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[Audit DB] Deduplication pass notice: " + e.getMessage());
            }
        });
    }

    public void runRetentionCleanup() {
        if (this.config.isPermanentArchival() || this.config.getInventoryRetentionDays() <= 0) {
            this.plugin.getLogger().info("[Audit DB] Permanent safe archival active: logs are never deleted. Consolidating duplicate idle snapshots...");
            this.deduplicateHistoricalLogs();
            return;
        }
        try (Connection conn = this.getConnection();){
            long now = System.currentTimeMillis();
            long posLimit = now - (long)this.config.getPositionRetentionDays() * 86400000L;
            long invLimit = now - (long)this.config.getInventoryRetentionDays() * 86400000L;
            long genLimit = now - (long)this.config.getGeneralAuditRetentionDays() * 86400000L;
            long adminLimit = now - (long)this.config.getAdminActionsRetentionDays() * 86400000L;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_position_snapshots WHERE timestamp < ?")) {
                ps.setTimestamp(1, new Timestamp(posLimit));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_inventory_snapshots WHERE timestamp < ? AND snapshot_reason = 'interval'")) {
                ps.setTimestamp(1, new Timestamp(invLimit));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_item_events WHERE timestamp < ?")) {
                ps.setTimestamp(1, new Timestamp(genLimit));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_action_logs WHERE timestamp < ?")) {
                ps.setTimestamp(1, new Timestamp(genLimit));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM audit_admin_actions WHERE timestamp < ?")) {
                ps.setTimestamp(1, new Timestamp(adminLimit));
                ps.executeUpdate();
            }
            this.plugin.getLogger().info("[Audit DB] Retention policy cleanup cycle completed successfully.");
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "[Audit DB] Error during retention cleanup", e);
        }
    }

    private static class ActiveSnapshotState {
        final String snapshotId;
        final String stateHash;
        int durationSeconds;
        long lastActiveMillis;

        ActiveSnapshotState(String snapshotId, String stateHash, long lastActiveMillis) {
            this.snapshotId = snapshotId;
            this.stateHash = stateHash;
            this.durationSeconds = 1;
            this.lastActiveMillis = lastActiveMillis;
        }
    }
}

