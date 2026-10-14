/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 */
package com.moonlight.vanilla.modules.teleport.listeners;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeleportListener
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;

    public TeleportListener(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        double threshold;
        if (!this.plugin.getConfig().getBoolean("teleport.cancel-on-movement", false)) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.plugin.getTeleportManager().hasActiveWarmup(player)) {
            return;
        }
        Location origin = this.plugin.getTeleportManager().getWarmupOrigin(player);
        if (origin == null || event.getTo() == null) {
            return;
        }
        double distance = origin.distanceSquared(event.getTo());
        if (distance > (threshold = this.plugin.getConfig().getDouble("teleport.movement-threshold", 0.15)) * threshold) {
            this.plugin.getTeleportManager().cancelWarmup(player, "You moved!");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (this.plugin.getTeleportManager().hasActiveWarmup(player)) {
                double minDamageHearts = this.plugin.getConfig().getDouble("teleport.min-damage-cancel-hearts", 5.0);
                double damageThreshold = minDamageHearts * 2.0;
                if (event.getFinalDamage() > damageThreshold) {
                    this.plugin.getTeleportManager().cancelWarmup(player, "You took heavy damage (" + String.format("%.1f", event.getFinalDamage() / 2.0) + " hearts > 5 hearts threshold)!");
                }
            }
        }
    }
}

