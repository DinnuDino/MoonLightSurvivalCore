/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.moonlight.vanilla.modules.teleport;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TeleportManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, ActiveWarmup> activeWarmups = new ConcurrentHashMap<UUID, ActiveWarmup>();

    public TeleportManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void cancelWarmup(Player player, String reason) {
        ActiveWarmup warmup = this.activeWarmups.remove(player.getUniqueId());
        if (warmup != null) {
            Player dest;
            warmup.task().cancel();
            player.sendMessage(MessageUtil.format("<red>Teleport cancelled: " + reason + "</red>"));
            if (warmup.targetUuid() != null && (dest = Bukkit.getPlayer((UUID)warmup.targetUuid())) != null && dest.isOnline() && !dest.getUniqueId().equals(player.getUniqueId())) {
                dest.sendMessage(MessageUtil.format("<red>Teleport cancelled: " + reason + "</red>"));
            }
        }
    }

    public boolean hasActiveWarmup(Player player) {
        return this.activeWarmups.containsKey(player.getUniqueId());
    }

    public Location getWarmupOrigin(Player player) {
        ActiveWarmup warmup = this.activeWarmups.get(player.getUniqueId());
        return warmup != null ? warmup.initialLoc() : null;
    }

    public void startWarmup(Player traveler, Player destination) {
        this.startWarmupInternal(traveler, destination.getUniqueId(), null, destination.getName());
    }

    public void startWarmup(Player traveler, Location destination, String destLabel) {
        this.startWarmupInternal(traveler, null, destination, destLabel);
    }

    private void startWarmupInternal(Player traveler, UUID targetUuid, Location targetLoc, String destLabel) {
        final BukkitTask[] taskHolder = new BukkitTask[1];
        Player destination;
        int warmupSec;
        int n = warmupSec = traveler.hasPermission("moonlight.teleport.bypass.warmup") || traveler.isOp() ? 0 : this.plugin.getConfig().getInt("teleport.warmup-seconds", 3);
        if (warmupSec <= 0) {
            Player destination2;
            if (targetLoc != null) {
                this.executeTeleport(traveler, targetLoc, destLabel);
            } else if (targetUuid != null && (destination2 = Bukkit.getPlayer((UUID)targetUuid)) != null && destination2.isOnline()) {
                this.executeTeleport(traveler, destination2.getLocation(), destination2.getName());
            }
            return;
        }
        ActiveWarmup existing = this.activeWarmups.remove(traveler.getUniqueId());
        if (existing != null) {
            existing.task().cancel();
        }
        int[] countdown = new int[]{warmupSec};
        traveler.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <yellow>Teleporting to " + destLabel + " in <gold><bold>" + countdown[0] + "</bold></gold>s... Moving will not cancel (only heavy damage > 5 hearts cancels)!</yellow>"));
        traveler.sendActionBar(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>\u2726 Teleporting in <gold><bold>" + countdown[0] + "</bold></gold>s... \u2726</gradient>"));
        try {
            traveler.playSound(traveler.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (targetUuid != null && !traveler.getUniqueId().equals(targetUuid) && (destination = Bukkit.getPlayer((UUID)targetUuid)) != null && destination.isOnline()) {
            destination.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <yellow>Teleport commencing in <gold><bold>" + warmupSec + "</bold></gold>s...</yellow>"));
        }
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!traveler.isOnline()) {
                if (taskHolder[0] != null) {
                    taskHolder[0].cancel();
                }
                this.activeWarmups.remove(traveler.getUniqueId());
                return;
            }
            countdown[0] = countdown[0] - 1;
            if (countdown[0] > 0) {
                traveler.sendMessage(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>[Moonlight]</gradient> <yellow>Teleporting in <gold><bold>" + countdown[0] + "</bold></gold>...</yellow>"));
                traveler.sendActionBar(MessageUtil.format("<gradient:#00d2ff:#3a7bd5>\u2726 Teleporting in <gold><bold>" + countdown[0] + "</bold></gold>... \u2726</gradient>"));
                try {
                    traveler.playSound(traveler.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f + 0.2f * (float)(warmupSec - countdown[0]));
                }
                catch (Throwable throwable) {}
            } else {
                if (taskHolder[0] != null) {
                    taskHolder[0].cancel();
                }
                this.activeWarmups.remove(traveler.getUniqueId());
                traveler.sendActionBar(MessageUtil.format("<green><bold>\u2726 Teleporting! \u2726</bold></green>"));
                try {
                    traveler.playSound(traveler.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                if (targetLoc != null) {
                    this.executeTeleport(traveler, targetLoc, destLabel);
                } else if (targetUuid != null) {
                    Player destPlayer = Bukkit.getPlayer(targetUuid);
                    if (destPlayer != null && destPlayer.isOnline()) {
                        this.executeTeleport(traveler, destPlayer.getLocation(), destPlayer.getName());
                    } else {
                        traveler.sendMessage(MessageUtil.format("<red>Target player is no longer online.</red>"));
                    }
                }
            }
        }, 20L, 20L);
        this.activeWarmups.put(traveler.getUniqueId(), new ActiveWarmup(taskHolder[0], traveler.getLocation().clone(), targetUuid, targetLoc, destLabel));
    }

    private void executeTeleport(Player traveler, Location destination, String destLabel) {
        traveler.teleportAsync(destination).thenAccept(success -> {
            if (success.booleanValue()) {
                traveler.sendMessage(MessageUtil.format("<green>Teleport complete!</green>"));
                this.lastTeleportTime.put(traveler.getUniqueId(), System.currentTimeMillis());
            } else {
                traveler.sendMessage(MessageUtil.format("<red>Teleportation failed.</red>"));
            }
        });
    }

    public void cancelAllPending() {
        for (ActiveWarmup warmup : this.activeWarmups.values()) {
            warmup.task().cancel();
        }
        this.activeWarmups.clear();
    }

    private record ActiveWarmup(BukkitTask task, Location initialLoc, UUID targetUuid, Location targetLoc, String destLabel) {
    }
}

