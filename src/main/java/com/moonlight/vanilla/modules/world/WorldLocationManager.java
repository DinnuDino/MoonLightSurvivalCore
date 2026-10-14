package com.moonlight.vanilla.modules.world;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * WorldLocationManager
 * Records and tracks the latest recorded location of each player in every world.
 * Seamlessly integrates with Multiverse-Core to ensure world teleports resume
 * exactly where the player left off instead of forcing them back to spawn.
 */
public class WorldLocationManager {

    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, Map<String, Location>> locationCache = new ConcurrentHashMap<>();

    public WorldLocationManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a location is within the spawn region of the server or the world.
     */
    public boolean isAtSpawn(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        World w = loc.getWorld();

        // 1. Check global server spawn (/spawn)
        if (plugin.getSpawnManager() != null) {
            Location serverSpawn = plugin.getSpawnManager().getSpawnLocation();
            if (serverSpawn != null && serverSpawn.getWorld() != null && serverSpawn.getWorld().equals(w)) {
                if (loc.distanceSquared(serverSpawn) <= 256.0) { // within 16 blocks of /spawn
                    return true;
                }
            }
        }

        // 2. Check vanilla world spawn
        Location worldSpawn = w.getSpawnLocation();
        if (loc.distanceSquared(worldSpawn) <= 144.0) { // within 12 blocks of vanilla spawn
            return true;
        }

        return false;
    }

    private boolean hasNonSpawnLocation(UUID uuid, String worldName) {
        Map<String, Location> map = locationCache.get(uuid);
        if (map != null) {
            Location loc = map.get(worldName.toLowerCase());
            if (loc != null && !isAtSpawn(loc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asynchronously loads all saved world locations for a player into cache.
     * Primes from both moonlight_world_locations and the audit logs (player_position_snapshots).
     */
    public CompletableFuture<Void> loadPlayerLocations(UUID uuid) {
        return plugin.getDatabaseManager().runAsync(() -> {
            Map<String, Location> worldMap = new ConcurrentHashMap<>();

            // 1. Load from moonlight_world_locations
            String sql = "SELECT world_name, x, y, z, yaw, pitch FROM moonlight_world_locations WHERE player_uuid = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String worldName = rs.getString("world_name");
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            double x = rs.getDouble("x");
                            double y = rs.getDouble("y");
                            double z = rs.getDouble("z");
                            float yaw = rs.getFloat("yaw");
                            float pitch = rs.getFloat("pitch");
                            Location loc = new Location(world, x, y, z, yaw, pitch);
                            worldMap.put(worldName.toLowerCase(), loc);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load world locations for " + uuid + ": " + e.getMessage());
            }

            // 2. Seed any missing worlds or non-spawn positions from moonlight_audit.db logs
            if (plugin.getAuditManager() != null && plugin.getAuditManager().getDatabaseManager() != null) {
                try (Connection aConn = plugin.getAuditManager().getDatabaseManager().getConnection();
                     PreparedStatement aps = aConn.prepareStatement(
                             "SELECT world_name, x, y, z, yaw, pitch FROM player_position_snapshots " +
                             "WHERE player_uuid = ? ORDER BY id DESC LIMIT 100")) {
                    aps.setString(1, uuid.toString());
                    try (ResultSet ars = aps.executeQuery()) {
                        while (ars.next()) {
                            String wName = ars.getString("world_name").toLowerCase();
                            Location existing = worldMap.get(wName);
                            if (existing == null) {
                                World w = Bukkit.getWorld(wName);
                                if (w != null) {
                                    Location loc = new Location(w, ars.getDouble("x"), ars.getDouble("y"), ars.getDouble("z"), ars.getFloat("yaw"), ars.getFloat("pitch"));
                                    worldMap.put(wName, loc);
                                }
                            } else if (isAtSpawn(existing)) {
                                World w = Bukkit.getWorld(wName);
                                if (w != null) {
                                    Location loc = new Location(w, ars.getDouble("x"), ars.getDouble("y"), ars.getDouble("z"), ars.getFloat("yaw"), ars.getFloat("pitch"));
                                    if (!isAtSpawn(loc)) {
                                        worldMap.put(wName, loc);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            locationCache.put(uuid, worldMap);
        });
    }

    /**
     * Saves a player's latest location in a world to cache and persists to database asynchronously.
     * Protects existing gameplay locations from being overwritten when standing at spawn.
     */
    public void saveLocation(UUID uuid, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        String worldName = loc.getWorld().getName().toLowerCase();

        // If player is standing right at spawn and already has a saved non-spawn location, do not overwrite it!
        if (isAtSpawn(loc) && hasNonSpawnLocation(uuid, worldName)) {
            return;
        }

        // Update fast memory cache immediately
        locationCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(worldName, loc.clone());

        // Asynchronously persist to SQLite/MySQL
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        long now = System.currentTimeMillis();

        plugin.getDatabaseManager().runAsync(() -> {
            String sql = "REPLACE INTO moonlight_world_locations " +
                         "(player_uuid, world_name, x, y, z, yaw, pitch, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, worldName);
                ps.setDouble(3, x);
                ps.setDouble(4, y);
                ps.setDouble(5, z);
                ps.setFloat(6, yaw);
                ps.setFloat(7, pitch);
                ps.setLong(8, now);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to persist world location for " + uuid + " in " + worldName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gets a player's latest recorded location in the specified world.
     * Queries:
     * 1. In-memory fast cache
     * 2. moonlight_world_locations database table
     * 3. player_position_snapshots in moonlight_audit.db (THE LOGS)
     * 4. player_inventory_snapshots in moonlight_audit.db
     * Returns null only if the player has never visited or recorded any activity in that world.
     */
    public Location getLastLocation(UUID uuid, String worldName) {
        if (worldName == null) return null;
        String wKey = worldName.toLowerCase(Locale.ROOT);

        // 1. In-memory fast cache
        Map<String, Location> map = locationCache.get(uuid);
        if (map != null) {
            Location loc = map.get(wKey);
            if (loc != null && loc.getWorld() != null) {
                return loc.clone();
            }
        }

        // 2. Direct DB query in moonlight_world_locations
        Location dbLoc = null;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT x, y, z, yaw, pitch FROM moonlight_world_locations WHERE player_uuid = ? AND world_name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, wKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        dbLoc = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
                    }
                }
            }
        } catch (Exception ignored) {}

        if (dbLoc != null && dbLoc.getWorld() != null) {
            locationCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(wKey, dbLoc);
            return dbLoc.clone();
        }

        // 3. Fallback: Query audit logs (player_position_snapshots in moonlight_audit.db)
        if (plugin.getAuditManager() != null && plugin.getAuditManager().getDatabaseManager() != null) {
            try (Connection aConn = plugin.getAuditManager().getDatabaseManager().getConnection();
                 PreparedStatement aps = aConn.prepareStatement(
                         "SELECT x, y, z, yaw, pitch FROM player_position_snapshots " +
                         "WHERE player_uuid = ? AND LOWER(world_name) = ? ORDER BY id DESC LIMIT 50")) {
                aps.setString(1, uuid.toString());
                aps.setString(2, wKey);
                try (ResultSet ars = aps.executeQuery()) {
                    while (ars.next()) {
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location loc = new Location(world, ars.getDouble("x"), ars.getDouble("y"), ars.getDouble("z"), ars.getFloat("yaw"), ars.getFloat("pitch"));
                            locationCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(wKey, loc);
                            saveLocation(uuid, loc);
                            return loc.clone();
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 4. Fallback 2: player_inventory_snapshots in moonlight_audit.db
            try (Connection aConn = plugin.getAuditManager().getDatabaseManager().getConnection();
                 PreparedStatement aps = aConn.prepareStatement(
                         "SELECT x, y, z FROM player_inventory_snapshots " +
                         "WHERE player_uuid = ? AND LOWER(world_name) = ? ORDER BY id DESC LIMIT 20")) {
                aps.setString(1, uuid.toString());
                aps.setString(2, wKey);
                try (ResultSet ars = aps.executeQuery()) {
                    while (ars.next()) {
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location loc = new Location(world, ars.getDouble("x"), ars.getDouble("y"), ars.getDouble("z"), 0.0f, 0.0f);
                            locationCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(wKey, loc);
                            saveLocation(uuid, loc);
                            return loc.clone();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Checks if a player has a recorded location in the specified world.
     */
    public boolean hasLastLocation(UUID uuid, String worldName) {
        return getLastLocation(uuid, worldName) != null;
    }

    /**
     * Cleans up player cache on disconnect.
     */
    public void unloadPlayer(UUID uuid) {
        locationCache.remove(uuid);
    }
}
