/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 */
package com.moonlight.vanilla.security;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SecurityAuditor
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private final boolean strictAntiOp;
    private final String allowedOwnerUuid;

    public SecurityAuditor(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.strictAntiOp = plugin.getConfig().getBoolean("security.strict-anti-auto-op", true);
        this.allowedOwnerUuid = plugin.getConfig().getString("security.owner-uuid", "00000000-0000-0000-0000-000000000000");
    }

    public boolean isAuthorizedOp(Player player) {
        if (player == null) {
            return false;
        }
        String uuidStr = player.getUniqueId().toString();
        if (this.allowedOwnerUuid != null && !this.allowedOwnerUuid.isEmpty() && !this.allowedOwnerUuid.equals("00000000-0000-0000-0000-000000000000") && uuidStr.equalsIgnoreCase(this.allowedOwnerUuid)) {
            return true;
        }
        return player.hasPermission("moonlight.core.admin") || player.hasPermission("moonlight.admin");
    }

    public boolean isAuthorizedOp(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
            return false;
        }
        String uuidStr = offlinePlayer.getUniqueId().toString();
        if (this.allowedOwnerUuid != null && !this.allowedOwnerUuid.isEmpty() && !this.allowedOwnerUuid.equals("00000000-0000-0000-0000-000000000000") && uuidStr.equalsIgnoreCase(this.allowedOwnerUuid)) {
            return true;
        }
        Player online = offlinePlayer.getPlayer();
        if (online != null) {
            return this.isAuthorizedOp(online);
        }
        return false;
    }

    public void enforceStartupAudit() {
        if (!this.strictAntiOp) {
            return;
        }
        this.plugin.getLogger().info("[SecurityAudit] Running startup Anti-Auto-OP audit...");
        int revoked = 0;
        for (OfflinePlayer opPlayer : Bukkit.getOperators()) {
            if (opPlayer.getUniqueId() == null || this.isAuthorizedOp(opPlayer) || this.allowedOwnerUuid.equals("00000000-0000-0000-0000-000000000000")) continue;
            opPlayer.setOp(false);
            ++revoked;
            this.plugin.getLogger().warning("[SECURITY VIOLATION] Revoked unauthorized OP status from: " + opPlayer.getName() + " (" + String.valueOf(opPlayer.getUniqueId()) + ")");
        }
        if (revoked > 0) {
            this.plugin.getLogger().warning("[SecurityAudit] Revoked OP privileges from " + revoked + " unauthorized accounts.");
        } else {
            this.plugin.getLogger().info("[SecurityAudit] Strict OP audit passed clean. Authorized owner recognized.");
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.isAuthorizedOp(player)) {
            if (!player.isOp()) {
                player.setOp(true);
                this.plugin.getLogger().info("[SECURITY] Authorized OP confirmed and granted for " + player.getName());
            }
            return;
        }
        if (this.strictAntiOp && player.isOp()) {
            player.setOp(false);
            this.plugin.getLogger().warning("[SECURITY ALERT] Unauthorized OP detected on join: " + player.getName() + " (" + String.valueOf(player.getUniqueId()) + "). Privilege revoked.");
            player.sendMessage(MessageUtil.format("<red><bold>[SECURITY]</bold> Unauthorized OP status was revoked per Moonlight Security Policy.</red>"));
        }
    }
}

