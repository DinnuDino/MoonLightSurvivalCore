package com.moonlight.vanilla.modules.wraps;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WrapInventoryHolder implements InventoryHolder {

    private final ItemStack heldItem;
    private final int page;
    private final int maxPages;
    private final boolean noWrap;
    private Inventory inventory;

    public WrapInventoryHolder(ItemStack heldItem, int page, int maxPages, boolean noWrap) {
        this.heldItem = heldItem;
        this.page = page;
        this.maxPages = maxPages;
        this.noWrap = noWrap;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public int getPage() {
        return page;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public boolean isNoWrap() {
        return noWrap;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
