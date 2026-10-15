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
            this.deployGeyserMappings();
        } else {
            this.deployGeyserMappings();
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

    public void deployGeyserMappings() {
        try {
            java.io.File[] candidateDirs = new java.io.File[] {
                new java.io.File("plugins/Geyser-Spigot/custom_mappings"),
                new java.io.File("plugins/Geyser-Paper/custom_mappings"),
                new java.io.File("plugins/geyser/custom_mappings"),
                new java.io.File("geyser/custom_mappings")
            };

            java.io.InputStream in = plugin.getResource("geyser_mappings/moonlight_custom_mappings.json");
            if (in == null) {
                in = plugin.getResource("moonlight_custom_mappings.json");
            }
            if (in == null) return;

            byte[] data = in.readAllBytes();
            in.close();

            for (java.io.File dir : candidateDirs) {
                java.io.File parent = dir.getParentFile();
                if (parent != null && parent.exists()) {
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    java.io.File target = new java.io.File(dir, "moonlight_custom_mappings.json");
                    java.nio.file.Files.write(target.toPath(), data, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
                    this.plugin.getLogger().info("[Crossplay] Deployed Geyser custom mappings to: " + target.getPath());
                }
            }
        } catch (Throwable t) {
            this.plugin.getLogger().log(java.util.logging.Level.WARNING, "[Crossplay] Note: Could not auto-deploy Geyser mappings: " + t.getMessage());
        }
    }
}

