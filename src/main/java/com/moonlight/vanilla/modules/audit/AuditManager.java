/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.moonlight.vanilla.modules.audit;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.AuditConfig;
import com.moonlight.vanilla.modules.audit.AuditDatabaseManager;
import com.moonlight.vanilla.modules.audit.listeners.AuditItemEventListener;
import com.moonlight.vanilla.modules.audit.listeners.AuditPositionTracker;
import com.moonlight.vanilla.modules.audit.listeners.AuditSessionListener;
import com.moonlight.vanilla.modules.audit.listeners.AuditWorldActionListener;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import java.io.File;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AuditManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private final AuditConfig config;
    private AuditDatabaseManager databaseManager;
    private InventorySerializer inventorySerializer;
    private BukkitTask positionTask;
    private BukkitTask inventoryTask;
    private BukkitTask retentionTask;

    public AuditManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.config = new AuditConfig();
    }

    public void initialize() {
        File configFile = new File(this.plugin.getDataFolder(), "audit.yml");
        if (!configFile.exists()) {
            this.plugin.saveResource("audit.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)configFile);
        this.config.load((FileConfiguration)yaml);
        if (!this.config.isEnabled()) {
            this.plugin.getLogger().info("[Audit] Audit subsystem is disabled via audit.yml.");
            return;
        }
        this.inventorySerializer = new InventorySerializer();
        this.databaseManager = new AuditDatabaseManager(this.plugin, this.config);
        this.databaseManager.initialize();
        Bukkit.getPluginManager().registerEvents((Listener)new AuditSessionListener(this.plugin, this.config, this.databaseManager), (Plugin)this.plugin);
        Bukkit.getPluginManager().registerEvents((Listener)new AuditItemEventListener(this.plugin, this.config, this.databaseManager, this.inventorySerializer), (Plugin)this.plugin);
        Bukkit.getPluginManager().registerEvents((Listener)new AuditWorldActionListener(this.plugin, this.config, this.databaseManager), (Plugin)this.plugin);
        this.positionTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AuditPositionTracker.recordSnapshot(player, "interval", this.databaseManager);
                if (this.plugin.getWorldLocationManager() == null || player.getWorld() == null) continue;
                this.plugin.getWorldLocationManager().saveLocation(player.getUniqueId(), player.getLocation());
            }
        }, 20L, 20L);
        long invIntervalTicks = (long)this.config.getInventorySnapshotIntervalSeconds() * 20L;
        this.inventoryTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.databaseManager.recordInventorySnapshot(player, "interval", null);
            }
        }, invIntervalTicks, invIntervalTicks);
        long retentionIntervalTicks = 864000L;
        this.retentionTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, () -> this.databaseManager.runRetentionCleanup(), 1200L, retentionIntervalTicks);
        this.plugin.getLogger().info("[Audit] Audit logging and snapshots (Interval: " + this.config.getPositionSnapshotIntervalSeconds() + "s) initialized.");
    }

    public void forceFlush() {
        if (this.databaseManager != null) {
            this.databaseManager.forceFlush();
        }
    }

    public void shutdown() {
        if (this.positionTask != null) {
            this.positionTask.cancel();
        }
        if (this.inventoryTask != null) {
            this.inventoryTask.cancel();
        }
        if (this.retentionTask != null) {
            this.retentionTask.cancel();
        }
        if (this.databaseManager != null) {
            this.plugin.getLogger().info("[Audit] Flushing queued audit records to database before shutdown...");
            this.databaseManager.shutdown();
        }
    }

    public MoonlightVanillaSurvivalCore getPlugin() {
        return this.plugin;
    }

    public AuditConfig getConfig() {
        return this.config;
    }

    public AuditDatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public InventorySerializer getInventorySerializer() {
        return this.inventorySerializer;
    }

    public void logAdminAction(UUID opUuid, String opName, String targetUuid, String targetName, String actionType, String reason, String sourceId, String itemsAffected, String result, String correlationId) {
        if (this.databaseManager != null) {
            this.databaseManager.logAdminAction(actionType, opUuid != null ? opUuid.toString() : null, opName, targetUuid, targetName, reason, sourceId, itemsAffected, result, correlationId);
        }
    }

    public void logAdminAction(UUID opUuid, String opName, UUID targetUuid, String targetName, String actionType, String reason, String sourceId, String itemsAffected, String result, String correlationId) {
        this.logAdminAction(opUuid, opName, targetUuid != null ? targetUuid.toString() : null, targetName, actionType, reason, sourceId, itemsAffected, result, correlationId);
    }
}

