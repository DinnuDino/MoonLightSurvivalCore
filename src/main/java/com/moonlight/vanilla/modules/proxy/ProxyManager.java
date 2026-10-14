/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.io.ByteArrayDataOutput
 *  com.google.common.io.ByteStreams
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.messaging.PluginMessageListener
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class ProxyManager
implements PluginMessageListener {
    private final MoonlightVanillaSurvivalCore plugin;
    private NetworkMode networkMode = NetworkMode.STANDALONE_PAPER;
    private String hubServer = "lobby";
    private boolean enforceVelocityHandshake = false;

    public ProxyManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String configuredMode = this.plugin.getConfig().getString("proxy.mode", "auto").toLowerCase();
        this.hubServer = this.plugin.getConfig().getString("proxy.hub-server", "lobby");
        this.enforceVelocityHandshake = this.plugin.getConfig().getBoolean("proxy.velocity.enforce-velocity-secret", false);
        this.networkMode = configuredMode.equals("velocity") ? NetworkMode.VELOCITY : (configuredMode.equals("bungeecord") ? NetworkMode.BUNGEECORD : (configuredMode.equals("standalone") ? NetworkMode.STANDALONE_PAPER : this.detectNetworkMode()));
        this.plugin.getLogger().info("[ProxyManager] Network operating mode active: " + this.networkMode.name());
        if (this.networkMode != NetworkMode.STANDALONE_PAPER) {
            try {
                this.plugin.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)this.plugin, "BungeeCord");
                this.plugin.getServer().getMessenger().registerIncomingPluginChannel((Plugin)this.plugin, "BungeeCord", (PluginMessageListener)this);
                this.plugin.getLogger().info("[ProxyManager] Plugin messaging channel 'BungeeCord' registered for " + this.networkMode.name() + ".");
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[ProxyManager] Failed to register plugin messaging channels: " + e.getMessage());
            }
        }
    }

    private NetworkMode detectNetworkMode() {
        YamlConfiguration yaml;
        try {
            File paperGlobal = new File("config/paper-global.yml");
            if (paperGlobal.exists()) {
                yaml = YamlConfiguration.loadConfiguration((File)paperGlobal);
                if (yaml.getBoolean("proxies.velocity.enabled", false)) {
                    return NetworkMode.VELOCITY;
                }
                if (yaml.getBoolean("proxies.bungee-cord.online-mode", false)) {
                    return NetworkMode.BUNGEECORD;
                }
            }
        }
        catch (Exception paperGlobal) {
            // empty catch block
        }
        try {
            File spigotYml = new File("spigot.yml");
            if (spigotYml.exists() && (yaml = YamlConfiguration.loadConfiguration((File)spigotYml)).getBoolean("settings.bungeecord", false)) {
                return NetworkMode.BUNGEECORD;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return NetworkMode.STANDALONE_PAPER;
    }

    public void shutdown() {
        if (this.networkMode != NetworkMode.STANDALONE_PAPER) {
            try {
                this.plugin.getServer().getMessenger().unregisterOutgoingPluginChannel((Plugin)this.plugin, "BungeeCord");
                this.plugin.getServer().getMessenger().unregisterIncomingPluginChannel((Plugin)this.plugin, "BungeeCord", (PluginMessageListener)this);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public boolean connectPlayer(Player player, String serverName) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (this.networkMode == NetworkMode.STANDALONE_PAPER) {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <yellow>This server is running in standalone Paper mode (no proxy network connected).</yellow>"));
            return false;
        }
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage((Plugin)this.plugin, "BungeeCord", out.toByteArray());
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Connecting to <yellow>" + serverName + "</yellow> via " + this.networkMode.name() + "...</green>"));
            return true;
        }
        catch (Exception e) {
            player.sendMessage(MessageUtil.format("<red>Failed to send proxy connection message: " + e.getMessage() + "</red>"));
            return false;
        }
    }

    public void sendToHub(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (this.networkMode == NetworkMode.STANDALONE_PAPER) {
            player.sendMessage(MessageUtil.format("<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Normal Paper Mode: Teleporting you to world spawn...</green>"));
            player.teleportAsync(player.getWorld().getSpawnLocation());
            return;
        }
        this.connectPlayer(player, this.hubServer);
    }

    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
    }

    public NetworkMode getNetworkMode() {
        return this.networkMode;
    }

    public boolean isProxyActive() {
        return this.networkMode != NetworkMode.STANDALONE_PAPER;
    }

    public String getHubServer() {
        return this.hubServer;
    }

    public static enum NetworkMode {
        VELOCITY,
        BUNGEECORD,
        STANDALONE_PAPER;

    }
}

