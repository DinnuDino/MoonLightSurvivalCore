/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 */
package com.moonlight.vanilla.modules.homes;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Home {
    private final UUID playerUuid;
    private final String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public Home(UUID playerUuid, String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.playerUuid = playerUuid;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static Home fromLocation(UUID playerUuid, String name, Location loc) {
        return new Home(playerUuid, name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public Location toLocation() {
        World world = Bukkit.getWorld((String)this.worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public String getName() {
        return this.name;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }
}

