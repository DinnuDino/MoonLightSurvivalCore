/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerAttemptPickupItemEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.moonlight.vanilla.modules.audit.listeners;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.AuditConfig;
import com.moonlight.vanilla.modules.audit.AuditDatabaseManager;
import com.moonlight.vanilla.modules.audit.listeners.AuditPositionTracker;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class AuditItemEventListener
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private final AuditConfig config;
    private final AuditDatabaseManager db;
    private final InventorySerializer serializer;

    public AuditItemEventListener(MoonlightVanillaSurvivalCore plugin, AuditConfig config, AuditDatabaseManager db, InventorySerializer serializer) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
        this.serializer = serializer;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (!this.config.isLogInventoryEvents()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        this.logItemEvent("pickup", player, item, "Item picked up from ground");
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!this.config.isLogInventoryEvents()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        this.logItemEvent("drop", player, item, "Item dropped to ground");
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String deathCorrelation = UUID.randomUUID().toString();
        this.db.recordInventorySnapshot(player, "death", deathCorrelation);
        AuditPositionTracker.recordSnapshot(player, "death", this.db);
        if (this.config.isLogInventoryEvents()) {
            for (ItemStack drop : event.getDrops()) {
                if (drop == null) continue;
                this.logItemEvent("death_drop", player, drop, "Dropped on death. Cause: " + event.getDeathMessage());
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.ENDER_CHEST) {
            this.db.recordInventorySnapshot(player, "echest_close", UUID.randomUUID().toString());
        } else if (type != InventoryType.CRAFTING && type != InventoryType.PLAYER) {
            this.db.recordInventorySnapshot(player, "container_close", UUID.randomUUID().toString());
        }
    }

    private void logItemEvent(String type, Player player, ItemStack item, String details) {
        Location loc = player.getLocation();
        String serialized = this.serializer.serializeItem(item);
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
        this.db.queueWrite(() -> {
            String sql = "INSERT INTO player_item_events (event_type, actor_uuid, actor_name, affected_player_uuid, affected_player_name, material, amount, item_display_name, item_serialized, world_name, x, y, z, correlation_id, details, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, type);
                ps.setString(2, player.getUniqueId().toString());
                ps.setString(3, player.getName());
                ps.setString(4, player.getUniqueId().toString());
                ps.setString(5, player.getName());
                ps.setString(6, item.getType().name());
                ps.setInt(7, item.getAmount());
                ps.setString(8, name);
                ps.setString(9, serialized);
                ps.setString(10, loc.getWorld().getName());
                ps.setDouble(11, loc.getX());
                ps.setDouble(12, loc.getY());
                ps.setDouble(13, loc.getZ());
                ps.setString(14, UUID.randomUUID().toString());
                ps.setString(15, details);
                ps.setTimestamp(16, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }
}

