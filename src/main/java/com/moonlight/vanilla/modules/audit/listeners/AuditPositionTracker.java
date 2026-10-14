/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package com.moonlight.vanilla.modules.audit.listeners;

import com.moonlight.vanilla.modules.audit.AuditDatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class AuditPositionTracker {
    private static final Map<UUID, ActivePositionState> activePositions = new ConcurrentHashMap<UUID, ActivePositionState>();

    public static void recordSnapshot(Player player, String reason, AuditDatabaseManager db) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Location loc = player.getLocation();
        if (loc.getWorld() == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        String worldUuid = loc.getWorld().getUID().toString();
        String worldName = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        String gamemode = player.getGameMode().name();
        double health = player.getHealth();
        int foodLevel = player.getFoodLevel();
        int expLevel = player.getLevel();
        String snapshotReason = reason != null ? reason : "interval";
        Timestamp now = Timestamp.from(Instant.now());
        db.queueWrite(() -> {
            String sql = "INSERT INTO player_position_snapshots (player_uuid, username, world_uuid, world_name, x, y, z, yaw, pitch, gamemode, health, food_level, exp_level, snapshot_reason, duration_seconds, last_active_timestamp, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setString(3, worldUuid);
                ps.setString(4, worldName);
                ps.setDouble(5, x);
                ps.setDouble(6, y);
                ps.setDouble(7, z);
                ps.setFloat(8, yaw);
                ps.setFloat(9, pitch);
                ps.setString(10, gamemode);
                ps.setDouble(11, health);
                ps.setInt(12, foodLevel);
                ps.setInt(13, expLevel);
                ps.setString(14, snapshotReason);
                ps.setInt(15, 1);
                ps.setTimestamp(16, now);
                ps.setTimestamp(17, now);
                ps.executeUpdate();
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }

    public static void clearPlayer(UUID uuid) {
        activePositions.remove(uuid);
    }

    private static class ActivePositionState {
        String worldName;
        int blockX;
        int blockY;
        int blockZ;
        int durationSeconds;
        long lastActiveMillis;

        ActivePositionState(String worldName, int blockX, int blockY, int blockZ) {
            this.worldName = worldName;
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.durationSeconds = 1;
            this.lastActiveMillis = System.currentTimeMillis();
        }
    }
}

