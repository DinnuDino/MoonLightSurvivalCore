/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.permissions.PermissionAttachmentInfo
 */
package com.moonlight.vanilla.modules.homes;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.homes.Home;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class HomeManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, List<Home>> homeCache = new ConcurrentHashMap<UUID, List<Home>>();

    public HomeManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<List<Home>> getHomes(UUID playerUuid) {
        List<Home> cached = this.homeCache.get(playerUuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(new ArrayList<Home>(cached));
        }
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Home> homes = new ArrayList<Home>();
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT home_name, world_name, x, y, z, yaw, pitch FROM moonlight_homes WHERE player_uuid = ?");){
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery();){
                    while (rs.next()) {
                        homes.add(new Home(playerUuid, rs.getString("home_name"), rs.getString("world_name"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
                    }
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to load homes for UUID: " + String.valueOf(playerUuid), e);
            }
            this.homeCache.put(playerUuid, homes);
            return homes;
        });
    }

    public List<String> getHomeNamesFast(UUID playerUuid) {
        List<Home> cached = this.homeCache.get(playerUuid);
        if (cached != null) {
            return cached.stream().map(Home::getName).toList();
        }
        this.getHomes(playerUuid);
        return Collections.emptyList();
    }

    public int getMaxHomes(Player player) {
        if (player.isOp() || player.hasPermission("moonlight.homes.unlimited") || player.hasPermission("moonlight.admin")) {
            return -1;
        }
        int max = this.plugin.getConfig().getInt("homes.max-homes-per-player", 5);
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String digits;
            String node;
            if (!pai.getValue() || !(node = pai.getPermission().toLowerCase(Locale.ROOT)).startsWith("moonlight.homes.") && !node.startsWith("moonlight.sethome.") || (digits = node.replaceAll("[^0-9]", "")).isEmpty()) continue;
            try {
                int customLimit = Integer.parseInt(digits);
                if (customLimit <= max) continue;
                max = customLimit;
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return max;
    }

    public CompletableFuture<Boolean> saveHome(Player player, String homeName, Location loc) {
        int homeLimit = this.getMaxHomes(player);
        return this.getHomes(player.getUniqueId()).thenCompose(existingHomes -> {
            boolean isOverwrite = existingHomes.stream().anyMatch(h -> h.getName().equalsIgnoreCase(homeName));
            if (!isOverwrite && homeLimit != -1 && existingHomes.size() >= homeLimit) {
                return CompletableFuture.completedFuture(false);
            }
            String homeId = String.valueOf(player.getUniqueId()) + ":" + homeName.toLowerCase();
            return CompletableFuture.supplyAsync(() -> {
                try (Connection conn = this.plugin.getDatabaseManager().getConnection();){
                    Boolean bl;
                    block14: {
                        PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO moonlight_homes (home_id, player_uuid, home_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        try {
                            ps.setString(1, homeId);
                            ps.setString(2, player.getUniqueId().toString());
                            ps.setString(3, homeName);
                            ps.setString(4, loc.getWorld().getName());
                            ps.setDouble(5, loc.getX());
                            ps.setDouble(6, loc.getY());
                            ps.setDouble(7, loc.getZ());
                            ps.setFloat(8, loc.getYaw());
                            ps.setFloat(9, loc.getPitch());
                            ps.executeUpdate();
                            Home savedHome = new Home(player.getUniqueId(), homeName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                            this.homeCache.compute(player.getUniqueId(), (k, list) -> {
                                ArrayList<Home> updated = list != null ? new ArrayList<Home>((Collection<Home>)list) : new ArrayList();
                                updated.removeIf(h -> h.getName().equalsIgnoreCase(homeName));
                                updated.add(savedHome);
                                return updated;
                            });
                            bl = true;
                            if (ps == null) break block14;
                        }
                        catch (Throwable t$) {
                            if (ps != null) {
                                try {
                                    ps.close();
                                }
                                catch (Throwable x2) {
                                    t$.addSuppressed(x2);
                                }
                            }
                            throw t$;
                        }
                        ps.close();
                    }
                    return bl;
                }
                catch (Exception e) {
                    this.plugin.getLogger().log(Level.SEVERE, "Failed to save home " + homeName, e);
                    return false;
                }
            });
        });
    }

    public CompletableFuture<Boolean> deleteHome(UUID playerUuid, String homeName) {
        String homeId = String.valueOf(playerUuid) + ":" + homeName.toLowerCase();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();){
                Boolean bl;
                block15: {
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM moonlight_homes WHERE home_id = ?");
                    try {
                        boolean ok;
                        ps.setString(1, homeId);
                        boolean bl2 = ok = ps.executeUpdate() > 0;
                        if (ok) {
                            this.homeCache.computeIfPresent(playerUuid, (k, list) -> {
                                ArrayList<Home> updated = new ArrayList<Home>((Collection<Home>)list);
                                updated.removeIf(h -> h.getName().equalsIgnoreCase(homeName));
                                return updated;
                            });
                        }
                        bl = ok;
                        if (ps == null) break block15;
                    }
                    catch (Throwable t$) {
                        if (ps != null) {
                            try {
                                ps.close();
                            }
                            catch (Throwable x2) {
                                t$.addSuppressed(x2);
                            }
                        }
                        throw t$;
                    }
                    ps.close();
                }
                return bl;
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to delete home " + homeName, e);
                return false;
            }
        });
    }

    public CompletableFuture<Integer> deleteAllHomes(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();){
                Integer n;
                block14: {
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM moonlight_homes WHERE player_uuid = ?");
                    try {
                        ps.setString(1, playerUuid.toString());
                        int count = ps.executeUpdate();
                        this.homeCache.remove(playerUuid);
                        n = count;
                        if (ps == null) break block14;
                    }
                    catch (Throwable t$) {
                        if (ps != null) {
                            try {
                                ps.close();
                            }
                            catch (Throwable x2) {
                                t$.addSuppressed(x2);
                            }
                        }
                        throw t$;
                    }
                    ps.close();
                }
                return n;
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to delete all homes for " + String.valueOf(playerUuid), e);
                return 0;
            }
        });
    }

    public Location resolveHomeDestination(Home home) {
        boolean suffocatingAtOrigin;
        Location loc = home.toLocation();
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        World world = loc.getWorld();
        int originX = loc.getBlockX();
        int originY = loc.getBlockY();
        int originZ = loc.getBlockZ();
        Block feet = world.getBlockAt(originX, originY, originZ);
        Block head = world.getBlockAt(originX, originY + 1, originZ);
        boolean bl = suffocatingAtOrigin = this.isSuffocating(feet) || this.isSuffocating(head);
        if (!suffocatingAtOrigin) {
            return loc;
        }
        int maxScanY = Math.min(world.getMaxHeight() - 2, originY + 32);
        for (int y = originY + 1; y <= maxScanY; ++y) {
            Block standOn = world.getBlockAt(originX, y - 1, originZ);
            Block candidateFeet = world.getBlockAt(originX, y, originZ);
            Block candidateHead = world.getBlockAt(originX, y + 1, originZ);
            if (!standOn.getType().isSolid() || this.isSuffocating(candidateFeet) || this.isSuffocating(candidateHead)) continue;
            Location adjusted = loc.clone();
            adjusted.setY((double)y);
            return adjusted;
        }
        boolean hasSafeAirIn5x5 = false;
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                for (int dy = -1; dy <= 3; ++dy) {
                    int testY = originY + dy;
                    if (testY < world.getMinHeight() || testY >= world.getMaxHeight() - 1) continue;
                    Block f = world.getBlockAt(originX + dx, testY, originZ + dz);
                    Block h = world.getBlockAt(originX + dx, testY + 1, originZ + dz);
                    if (this.isSuffocating(f) || this.isSuffocating(h)) continue;
                    hasSafeAirIn5x5 = true;
                    break;
                }
                if (hasSafeAirIn5x5) break;
            }
            if (hasSafeAirIn5x5) break;
        }
        if (!hasSafeAirIn5x5) {
            return null;
        }
        for (int r = 1; r <= 2; ++r) {
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    for (int y = originY; y <= originY + 10; ++y) {
                        Block standOn = world.getBlockAt(originX + dx, y - 1, originZ + dz);
                        Block candidateFeet = world.getBlockAt(originX + dx, y, originZ + dz);
                        Block candidateHead = world.getBlockAt(originX + dx, y + 1, originZ + dz);
                        if (!standOn.getType().isSolid() || this.isSuffocating(candidateFeet) || this.isSuffocating(candidateHead)) continue;
                        Location adjusted = loc.clone();
                        adjusted.setX((double)(originX + dx) + 0.5);
                        adjusted.setY((double)y);
                        adjusted.setZ((double)(originZ + dz) + 0.5);
                        return adjusted;
                    }
                }
            }
        }
        return null;
    }

    public boolean isSuffocating(Block block) {
        if (block == null) {
            return false;
        }
        Material mat = block.getType();
        if (mat.isAir() || !mat.isSolid()) {
            return false;
        }
        if (block.isPassable()) {
            return false;
        }
        return mat.isOccluding();
    }

    public boolean isLocationSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        return !this.isSuffocating(feet) && !this.isSuffocating(head);
    }
}

