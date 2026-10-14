package com.moonlight.vanilla.modules.ranks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * RanksInventoryHolder
 * Dedicated InventoryHolder for the /ranks paid ranks GUI.
 */
public class RanksInventoryHolder implements InventoryHolder {

    private final Player player;
    private Inventory inventory;

    public RanksInventoryHolder(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
