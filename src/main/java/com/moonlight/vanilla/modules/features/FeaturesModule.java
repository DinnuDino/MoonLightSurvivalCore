/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package com.moonlight.vanilla.modules.features;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class FeaturesModule
implements Listener,
CommandExecutor {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, Long> lastMove = new HashMap<UUID, Long>();

    public FeaturesModule(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
        Bukkit.getScheduler().runTaskTimer((Plugin)plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                long last;
                if (player.hasPermission("moonlight.noafkkick") || now - (last = this.lastMove.getOrDefault(player.getUniqueId(), now).longValue()) <= 900000L) continue;
                Bukkit.getScheduler().runTask((Plugin)plugin, () -> player.kick(MessageUtil.format("<red>Kicked for being AFK for more than 15 minutes.</red>")));
            }
        }, 1200L, 1200L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            this.lastMove.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        this.lastMove.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        this.lastMove.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (e.getEntity().hasPermission("moonlight.keepxp")) {
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player)sender;
        switch (command.getName().toLowerCase()) {
            case "workbench": 
            case "wb": {
                if (player.hasPermission("moonlight.workbench") || player.hasPermission("moonlight.admin") || player.isOp()) {
                    player.openWorkbench(null, true);
                    break;
                }
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cYou are forbidden to use this."));
                break;
            }
            case "hat": {
                if (player.hasPermission("moonlight.hat") || player.hasPermission("moonlight.admin") || player.isOp()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) {
                        player.sendMessage(MessageUtil.format("<red>You must hold an item.</red>"));
                        break;
                    }
                    ItemStack helm = player.getInventory().getHelmet();
                    player.getInventory().setHelmet(hand);
                    player.getInventory().setItemInMainHand(helm);
                    player.sendMessage(MessageUtil.format("<green>Enjoy your new hat!</green>"));
                    break;
                }
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cYou are forbidden to use this."));
                break;
            }
            case "repair": {
                if (player.hasPermission("moonlight.repair") || player.hasPermission("moonlight.admin") || player.isOp()) {
                    ItemMeta itemMeta;
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() != Material.AIR && (itemMeta = hand.getItemMeta()) instanceof Damageable) {
                        Damageable damageable = (Damageable)itemMeta;
                        damageable.setDamage(0);
                        hand.setItemMeta((ItemMeta)damageable);
                        player.sendMessage(MessageUtil.format("<green>Item repaired!</green>"));
                        break;
                    }
                    player.sendMessage(MessageUtil.format("<yellow>This item cannot be repaired.</yellow>"));
                    break;
                }
                player.sendMessage(MessageUtil.format("&9MoonLight &8\u2759 &cYou are forbidden to use this."));
            }
        }
        return true;
    }
}

