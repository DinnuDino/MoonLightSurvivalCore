/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 */
package com.moonlight.vanilla.modules.audit.listeners;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.AuditConfig;
import com.moonlight.vanilla.modules.audit.AuditDatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class AuditWorldActionListener
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private final AuditConfig config;
    private final AuditDatabaseManager db;

    public AuditWorldActionListener(MoonlightVanillaSurvivalCore plugin, AuditConfig config, AuditDatabaseManager db) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        this.logAction("block_place", player, loc, event.getBlock().getType().name(), null);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        this.logAction("block_break", player, loc, event.getBlock().getType().name(), null);
    }

    private void logAction(String eventType, Player player, Location loc, String targetInfo, String details) {
        this.db.queueWrite(() -> {
            String sql = "INSERT INTO player_action_logs (event_type, player_uuid, username, world_name, x, y, z, target_info, details, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, eventType);
                ps.setString(2, player.getUniqueId().toString());
                ps.setString(3, player.getName());
                ps.setString(4, loc.getWorld().getName());
                ps.setDouble(5, loc.getX());
                ps.setDouble(6, loc.getY());
                ps.setDouble(7, loc.getZ());
                ps.setString(8, targetInfo);
                ps.setString(9, details);
                ps.setTimestamp(10, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }
}

