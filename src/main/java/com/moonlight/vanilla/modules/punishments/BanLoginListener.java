/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent$Result
 */
package com.moonlight.vanilla.modules.punishments;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.BanManager;
import com.moonlight.vanilla.modules.punishments.BanRecord;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanLoginListener
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;

    public BanLoginListener(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        BanManager banManager = this.plugin.getBanManager();
        if (banManager == null) {
            return;
        }
        BanRecord ban = banManager.getActiveBan(uuid);
        if (ban == null) {
            ban = banManager.getActiveBanByName(name);
        }
        if (ban == null) {
            return;
        }
        if (ban.isExpired()) {
            banManager.expireBan(ban);
            this.plugin.getLogger().info("[Punishments] Allowed connection for " + name + " (Ban " + ban.getBanId() + " expired).");
            return;
        }
        String kickScreen = banManager.buildKickScreen(ban);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.format(kickScreen));
        this.plugin.getLogger().info("[Punishments] Blocked login for banned player " + name + " [Ban ID: " + ban.getBanId() + " | Time Left: " + ban.getFormattedRemaining() + "]");
    }
}

