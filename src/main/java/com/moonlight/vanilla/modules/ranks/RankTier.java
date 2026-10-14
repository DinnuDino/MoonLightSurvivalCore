package com.moonlight.vanilla.modules.ranks;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

/**
 * RankTier
 * Model representing a paid donor rank on Moonlight SMP.
 */
public class RankTier {

    private final String id;
    private final String name;
    private final String displayName;
    private final String price;
    private final Material material;
    private final int slot;
    private final String description;
    private final String permission;
    private final List<String> perks;

    public RankTier(String id, String name, String displayName, String price, Material material, int slot, String description, String permission, List<String> perks) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.price = price;
        this.material = material != null ? material : Material.EMERALD;
        this.slot = slot;
        this.description = description != null ? description : "";
        this.permission = permission != null ? permission : "moonlight.wraps.tier." + id.toLowerCase();
        this.perks = perks != null ? perks : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrice() {
        return price;
    }

    public Material getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getPerks() {
        return perks;
    }
}
