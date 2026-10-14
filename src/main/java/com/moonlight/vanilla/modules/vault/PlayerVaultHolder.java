package com.moonlight.vanilla.modules.vault;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * PlayerVaultHolder
 * Represents the InventoryHolder for a persistent Player Vault (54 slots).
 */
public class PlayerVaultHolder implements InventoryHolder {

    private final UUID ownerUuid;
    private final String ownerName;
    private final int vaultNumber;
    private Inventory inventory;

    public PlayerVaultHolder(UUID ownerUuid, String ownerName, int vaultNumber) {
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.vaultNumber = vaultNumber;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getVaultNumber() {
        return vaultNumber;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
