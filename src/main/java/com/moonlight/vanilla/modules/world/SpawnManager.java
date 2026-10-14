package com.moonlight.vanilla.modules.world;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * SpawnManager
 * Handles the global server spawn point, /setspawn, and guarantees that every player
 * spawns at /spawn upon joining the server.
 */
public class SpawnManager {

    private final MoonlightVanillaSurvivalCore plugin;
    private Location spawnLocation;

    public SpawnManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    public void loadSpawn() {
        FileConfiguration config = plugin.getConfig();
        String worldName = config.getString("spawn.world");
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = config.getDouble("spawn.x");
                double y = config.getDouble("spawn.y");
                double z = config.getDouble("spawn.z");
                float yaw = (float) config.getDouble("spawn.yaw", 0.0);
                float pitch = (float) config.getDouble("spawn.pitch", 0.0);
                this.spawnLocation = new Location(world, x, y, z, yaw, pitch);
                return;
            }
        }

        // Fallback: Default main world spawn
        if (!Bukkit.getWorlds().isEmpty()) {
            this.spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation().clone().add(0.5, 0.0, 0.5);
        }
    }

    public Location getSpawnLocation() {
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            loadSpawn();
        }
        return spawnLocation != null ? spawnLocation.clone() : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    public void setSpawn(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.spawnLocation = loc.clone();

        FileConfiguration config = plugin.getConfig();
        config.set("spawn.world", loc.getWorld().getName());
        config.set("spawn.x", loc.getX());
        config.set("spawn.y", loc.getY());
        config.set("spawn.z", loc.getZ());
        config.set("spawn.yaw", (double) loc.getYaw());
        config.set("spawn.pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    public void teleportToSpawn(Player player) {
        if (player == null || !player.isOnline()) return;

        // 1. Dispatch /spawn command (handled by Multiverse-Core / Essentials)
        boolean executed = player.performCommand("spawn");
        if (!executed) {
            // 2. Fallback: Multiverse console command /mv spawn <player>
            boolean mvRan = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv spawn " + player.getName());
            if (!mvRan) {
                // 3. Fallback to default world spawn if neither command succeeded
                Location spawn = getSpawnLocation();
                if (spawn != null) {
                    player.teleportAsync(spawn);
                }
            }
        }
    }

    public boolean isTeleportOnJoinEnabled() {
        return plugin.getConfig().getBoolean("spawn.teleport-on-join", true);
    }
}
