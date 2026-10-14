/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package com.moonlight.vanilla.modules.audit.listeners;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.AuditConfig;
import com.moonlight.vanilla.modules.audit.AuditDatabaseManager;
import com.moonlight.vanilla.modules.audit.listeners.AuditPositionTracker;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuditSessionListener
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private final AuditConfig config;
    private final AuditDatabaseManager databaseManager;
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, String> sessionIds = new ConcurrentHashMap<UUID, String>();

    public AuditSessionListener(MoonlightVanillaSurvivalCore plugin, AuditConfig config, AuditDatabaseManager databaseManager) {
        this.plugin = plugin;
        this.config = config;
        this.databaseManager = databaseManager;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        InetSocketAddress addr;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();
        this.joinTimes.put(uuid, now);
        this.sessionIds.put(uuid, sessionId);
        String platform = "JAVA";
        if (this.plugin.getBedrockManager() != null && this.plugin.getBedrockManager().isBedrockPlayer(player)) {
            platform = "BEDROCK";
        }
        String ip = null;
        if (this.config.isTrackIpAddresses() && (addr = player.getAddress()) != null && addr.getAddress() != null) {
            ip = addr.getAddress().getHostAddress();
        }
        String finalIp = ip;
        String finalPlatform = platform;
        this.databaseManager.queueWrite(() -> {
            String sql = "INSERT INTO player_sessions (session_id, player_uuid, username, platform, ip_address, joined_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = this.databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, sessionId);
                ps.setString(2, uuid.toString());
                ps.setString(3, player.getName());
                ps.setString(4, finalPlatform);
                ps.setString(5, finalIp);
                ps.setTimestamp(6, Timestamp.from(Instant.ofEpochMilli(now)));
                ps.executeUpdate();
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[Audit] Failed to log join session: " + e.getMessage());
            }
        });
        AuditPositionTracker.recordSnapshot(player, "join", this.databaseManager);
        this.databaseManager.recordInventorySnapshot(player, "join", sessionId);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        long duration;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Long joinedAt = this.joinTimes.remove(uuid);
        String sessionId = this.sessionIds.remove(uuid);
        long now = System.currentTimeMillis();
        long l = duration = joinedAt != null ? (now - joinedAt) / 1000L : 0L;
        if (sessionId != null) {
            this.databaseManager.queueWrite(() -> {
                String sql = "UPDATE player_sessions SET quit_at = ?, duration_seconds = ? WHERE session_id = ?";
                try (Connection conn = this.databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setTimestamp(1, Timestamp.from(Instant.ofEpochMilli(now)));
                    ps.setLong(2, duration);
                    ps.setString(3, sessionId);
                    ps.executeUpdate();
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("[Audit] Failed to update quit session: " + e.getMessage());
                }
            });
        }
        AuditPositionTracker.recordSnapshot(player, "quit", this.databaseManager);
        this.databaseManager.recordInventorySnapshot(player, "quit", sessionId);
    }
}

