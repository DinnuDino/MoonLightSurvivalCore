/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package com.moonlight.vanilla.modules.crossplay;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BedrockManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private boolean floodgatePresent = false;
    private boolean geyserPresent = false;
    private boolean viaVersionPresent = false;
    private boolean viaBackwardsPresent = false;

    public BedrockManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.floodgatePresent = Bukkit.getPluginManager().isPluginEnabled("floodgate");
        this.geyserPresent = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot");
        this.viaVersionPresent = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
        this.viaBackwardsPresent = Bukkit.getPluginManager().isPluginEnabled("ViaBackwards");
        if (this.floodgatePresent) {
            this.plugin.getLogger().info("[Crossplay] Floodgate 2.0 detected! Bedrock authentication enabled (No Java account needed).");
        } else {
            this.plugin.getLogger().info("[Crossplay] Running in standalone mode. Install Floodgate & Geyser for Bedrock crossplay.");
        }
        if (this.geyserPresent) {
            this.plugin.getLogger().info("[Crossplay] Geyser-Spigot detected! Translating Bedrock protocols to Paper 26.2.");
        }
        if (this.viaVersionPresent || this.viaBackwardsPresent) {
            this.plugin.getLogger().info("[Crossplay] ViaVersion / ViaBackwards active! Legacy Java client backward compatibility enabled (1.8.9 - 1.20.x).");
        }
    }

    public boolean isViaVersionPresent() {
        return this.viaVersionPresent;
    }

    public boolean isViaBackwardsPresent() {
        return this.viaBackwardsPresent;
    }

    public boolean isBedrockPlayer(Player player) {
        if (player == null) {
            return false;
        }
        return this.isBedrockUuid(player.getUniqueId()) || player.getName().startsWith("*") || player.getName().startsWith(".");
    }

    public boolean isBedrockUuid(UUID uuid) {
        if (!this.floodgatePresent || uuid == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object apiInstance = apiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Object result = apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(apiInstance, uuid);
            return Boolean.TRUE.equals(result);
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    public String getDeviceName(Player player) {
        if (!this.isBedrockPlayer(player)) {
            return "Java Edition";
        }
        if (!this.floodgatePresent) {
            return "Bedrock Edition (Generic)";
        }
        try {
            Object deviceOs;
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object apiInstance = apiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Object floodgatePlayer = apiClass.getMethod("getPlayer", UUID.class).invoke(apiInstance, player.getUniqueId());
            if (floodgatePlayer != null && (deviceOs = floodgatePlayer.getClass().getMethod("getDeviceOs", new Class[0]).invoke(floodgatePlayer, new Object[0])) != null) {
                return deviceOs.toString();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "Bedrock Edition";
    }

    public boolean isFloodgatePresent() {
        return this.floodgatePresent;
    }

    public boolean isGeyserPresent() {
        return this.geyserPresent;
    }
}

