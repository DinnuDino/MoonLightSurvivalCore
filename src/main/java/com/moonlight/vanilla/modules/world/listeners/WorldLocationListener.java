package com.moonlight.vanilla.modules.world.listeners;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorldLocationListener
 * 1. Spawns players at /spawn upon joining (if configured).
 * 2. Continuously records player departure locations per-world (avoiding spawn overwrites).
 * 3. STRICTLY intercepts ONLY the Citizens NPC Multiverse world teleport command:
 *    (/mvtp <world>, /mv teleport <world>) to restore the player's last saved location in that world!
 *    NEVER interferes with /spawn, /tp, /teleport, /home, /warp, /back, /tpa, or portals!
 */
public class WorldLocationListener implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, MvTeleportIntent> pendingMvTeleports = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> joiningPlayers = ConcurrentHashMap.newKeySet();

    private static class MvTeleportIntent {
        final String targetWorld;
        final long timestamp;

        MvTeleportIntent(String targetWorld, long timestamp) {
            this.targetWorld = targetWorld != null ? targetWorld.toLowerCase(Locale.ROOT) : "";
            this.timestamp = timestamp;
        }
    }

    public WorldLocationListener(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns every player at /spawn on join, and primes their per-world location cache.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        joiningPlayers.add(uuid);

        // 1. Prime per-world location cache from database & audit logs
        plugin.getWorldLocationManager().loadPlayerLocations(uuid);

        // 2. Guarantee player spawns at /spawn on join
        if (plugin.getSpawnManager().isTeleportOnJoinEnabled()) {
            Location spawn = plugin.getSpawnManager().getSpawnLocation();
            if (spawn != null) {
                plugin.getSpawnManager().teleportToSpawn(player);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getSpawnManager().teleportToSpawn(player);
                    }
                }, 1L);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getSpawnManager().teleportToSpawn(player);
                    }
                }, 5L);
            }
        }

        // Synchronize client Brigadier command completion and preview trees
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.updateCommands();
            }
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.updateCommands();
            }
        }, 10L);

        // Release joining guard after initial spawn placement completes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            joiningPlayers.remove(uuid);
        }, 15L);
    }

    /**
     * Saves the player's last location in their current world when quitting.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        joiningPlayers.remove(uuid);

        // Save current location in current world if alive (WorldLocationManager protects spawn)
        if (!player.isDead()) {
            plugin.getWorldLocationManager().saveLocation(uuid, player.getLocation());
        }
        pendingMvTeleports.remove(uuid);
    }

    /**
     * Helper to detect any Multiverse teleport command targeting world 'world':
     * Supports: /mvtp world, /mv tp world, /mv goto world, /mvgoto world, /mv teleport world,
     * /multiverse-core:mvtp world, flags like -p, etc.
     */
    private boolean isMultiverseWorldCommand(String raw) {
        if (raw == null) return false;
        String clean = raw.startsWith("/") ? raw.substring(1).trim().toLowerCase(Locale.ROOT) : raw.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith("multiverse-core:")) {
            clean = clean.substring("multiverse-core:".length()).trim();
        } else if (clean.startsWith("mv:")) {
            clean = clean.substring("mv:".length()).trim();
        }

        boolean isMv = clean.startsWith("mvtp") || clean.startsWith("mv tp") ||
                       clean.startsWith("mv goto") || clean.startsWith("mvgoto") ||
                       clean.startsWith("mv teleport");

        if (!isMv) return false;

        String[] parts = clean.split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("world")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects player-executed Multiverse teleport commands from Citizens NPCs (-p flag):
     * e.g. /npc cmd add -p mvtp <world> OR /mv teleport <world>
     * Clears pending intents if the player runs any normal teleport command (/spawn, /tp, /home, etc.)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().trim();
        String msg = raw.startsWith("/") ? raw.substring(1).trim().toLowerCase(Locale.ROOT) : raw.trim().toLowerCase(Locale.ROOT);

        if (msg.startsWith("multiverse-core:")) {
            msg = msg.substring("multiverse-core:".length()).trim();
        } else if (msg.startsWith("mv:")) {
            msg = msg.substring("mv:".length()).trim();
        }

        // Normal commands: Guarantee NO pending Multiverse redirect exists!
        if (msg.startsWith("spawn") || msg.startsWith("tp ") || msg.startsWith("teleport ") ||
            msg.startsWith("tpa ") || msg.startsWith("tpahere ") || msg.startsWith("home") ||
            msg.startsWith("warp") || msg.startsWith("back") || msg.startsWith("rtp") || msg.startsWith("wild")) {
            pendingMvTeleports.remove(event.getPlayer().getUniqueId());
            return;
        }

        // Specifically detect Citizens NPC / player Multiverse world teleport targeting world 'world':
        if (isMultiverseWorldCommand(raw)) {
            Player p = event.getPlayer();
            if (p.getWorld() != null && p.getWorld().getName().equalsIgnoreCase("world")) {
                plugin.getWorldLocationManager().saveLocation(p.getUniqueId(), p.getLocation());
            }
            pendingMvTeleports.put(p.getUniqueId(), new MvTeleportIntent("world", System.currentTimeMillis()));
        }
    }

    /**
     * Detects server/console executed NPC commands:
     * e.g. /npc cmd add mvtp <p> world
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String raw = event.getCommand().trim();
        if (isMultiverseWorldCommand(raw)) {
            String[] parts = raw.split("\\s+");
            for (String part : parts) {
                Player p = Bukkit.getPlayerExact(part);
                if (p != null) {
                    if (p.getWorld() != null && p.getWorld().getName().equalsIgnoreCase("world")) {
                        plugin.getWorldLocationManager().saveLocation(p.getUniqueId(), p.getLocation());
                    }
                    pendingMvTeleports.put(p.getUniqueId(), new MvTeleportIntent("world", System.currentTimeMillis()));
                }
            }
        }
    }

    /**
     * Intercepts teleports:
     * - Records departure location in the world being left when crossing worlds.
     * - Redirects ONLY if the teleport was triggered by that specific Citizens NPC Multiverse command targeting 'world'!
     * - Never redirects arbitrary /tp, /teleport, /tpa, /home, /spawn, /warp, /back, or portal teleports!
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == null || to == null || to.getWorld() == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();

        if (joiningPlayers.contains(uuid)) return;

        boolean isCrossWorld = !fromWorld.equals(toWorld);

        // Record departure location in fromWorld if leaving for another world
        if (isCrossWorld) {
            plugin.getWorldLocationManager().saveLocation(uuid, from);
        }

        // STRICT CHECK: ONLY redirect if there was an explicit pending Multiverse NPC teleport intent targeting 'world'!
        MvTeleportIntent intent = pendingMvTeleports.remove(uuid);
        if (intent == null) {
            // Normal teleport (/spawn, /tp, /home, /warp, etc.) - DO NOT REDIRECT!
            return;
        }

        // Must be recent (within 15 seconds)
        if (System.currentTimeMillis() - intent.timestamp > 15000L) {
            return;
        }

        // Must strictly match the destination world "world"
        if (!toWorld.getName().equalsIgnoreCase("world") || !intent.targetWorld.equalsIgnoreCase("world")) {
            return;
        }

        boolean resumeEnabled = plugin.getConfig().getBoolean("multiverse.resume-latest-location", true);
        if (!resumeEnabled) return;

        Location lastLoc = plugin.getWorldLocationManager().getLastLocation(uuid, toWorld.getName());
        if (lastLoc != null && lastLoc.getWorld() != null) {
            event.setTo(lastLoc);

            // Follow-up safety tasks to override Multiverse-Core SafeTeleporter post-teleport adjustments
            for (long delay : new long[]{1L, 5L, 15L}) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.getWorld().getName().equalsIgnoreCase("world")) {
                        Location cur = player.getLocation();
                        if (cur.distanceSquared(lastLoc) > 4.0) {
                            player.teleportAsync(lastLoc);
                        }
                    }
                }, delay);
            }

            player.sendMessage(MessageUtil.format(
                    "<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <green>Teleported to your latest location in <white>" +
                    toWorld.getName() + "</white>!</green>"
            ));
        } else {
            player.sendMessage(MessageUtil.format(
                    "<gradient:#6366f1:#a855f7>[Moonlight]</gradient> <yellow>Welcome to <white>" +
                    toWorld.getName() + "</white>! Teleporting to world spawn (first visit).</yellow>"
            ));
        }
    }
}
