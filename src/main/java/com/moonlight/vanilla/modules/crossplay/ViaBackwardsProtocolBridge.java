/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 */
package com.moonlight.vanilla.modules.crossplay;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ViaBackwardsProtocolBridge
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private boolean viaVersionPresent = false;
    private boolean viaBackwardsPresent = false;
    private boolean viaRewindPresent = false;

    public ViaBackwardsProtocolBridge(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.viaVersionPresent = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
        this.viaBackwardsPresent = Bukkit.getPluginManager().isPluginEnabled("ViaBackwards");
        this.viaRewindPresent = Bukkit.getPluginManager().isPluginEnabled("ViaRewind");
        if (this.viaVersionPresent && this.viaBackwardsPresent) {
            this.plugin.getLogger().info("[ViaPipeline] ViaVersion & ViaBackwards detected! Java 1.18.1 through 26.2 pipeline online.");
        } else if (this.viaVersionPresent) {
            this.plugin.getLogger().warning("[ViaPipeline] ViaVersion detected, but ViaBackwards is missing! Older clients (1.18-1.20) will NOT be able to connect.");
        } else {
            this.plugin.getLogger().info("[ViaPipeline] Running in native protocol mode (Install ViaVersion + ViaBackwards for multi-version support).");
        }
        if (this.viaRewindPresent) {
            this.plugin.getLogger().info("[ViaPipeline] ViaRewind detected! Legacy 1.7.x - 1.12.2 compatibility active.");
        }
    }

    public int getPlayerProtocolVersion(Player player) {
        if (!this.viaVersionPresent || player == null) {
            return -1;
        }
        try {
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = viaClass.getMethod("getAPI", new Class[0]).invoke(null, new Object[0]);
            Method getPlayerVersion = api.getClass().getMethod("getPlayerVersion", UUID.class);
            Object versionObj = getPlayerVersion.invoke(api, player.getUniqueId());
            if (versionObj instanceof Integer) {
                Integer protocol = (Integer)versionObj;
                return protocol;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return -1;
    }

    public String getFriendlyVersionName(int protocol) {
        return switch (protocol) {
            case 769 -> "26.2 / 1.21.4 (Latest Native)";
            case 768 -> "1.21.2 - 1.21.3 (Bundles)";
            case 767 -> "1.21.0 - 1.21.1 (Tricky Trials)";
            case 766 -> "1.20.5 - 1.20.6 (Armadillo/Components)";
            case 765 -> "1.20.3 - 1.20.4";
            case 764 -> "1.20.2";
            case 763 -> "1.20.0 - 1.20.1 (Trails & Tales)";
            case 762 -> "1.19.4";
            case 761 -> "1.19.3";
            case 760 -> "1.19.1 - 1.19.2";
            case 759 -> "1.19.0 (The Wild Update)";
            case 758 -> "1.18.2 (Caves & Cliffs II)";
            case 757 -> "1.18.0 - 1.18.1";
            case 756 -> "1.17.1";
            case 755 -> "1.17.0";
            case 754 -> "1.16.4 - 1.16.5";
            case 340 -> "1.12.2 (Legacy Modding)";
            case 47 -> "1.8.9 (Legacy PvP)";
            default -> protocol > 769 ? "Future / Snapshot (" + protocol + ")" : "Protocol #" + protocol;
        };
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getBedrockManager().isBedrockPlayer(player)) {
            return;
        }
        int protocol = this.getPlayerProtocolVersion(player);
        if (protocol > 0) {
            String versionName = this.getFriendlyVersionName(protocol);
            this.plugin.getLogger().info("[ViaPipeline] " + player.getName() + " connected with Java Protocol " + protocol + " (" + versionName + ")");
        }
    }

    public boolean isViaVersionPresent() {
        return this.viaVersionPresent;
    }

    public boolean isViaBackwardsPresent() {
        return this.viaBackwardsPresent;
    }

    public boolean isViaRewindPresent() {
        return this.viaRewindPresent;
    }
}

