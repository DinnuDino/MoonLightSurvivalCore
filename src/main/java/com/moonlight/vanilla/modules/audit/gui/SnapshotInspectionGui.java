package com.moonlight.vanilla.modules.audit.gui;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.commands.LogsCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * InteractiveChat-Style Inventory Inspection GUI.
 * Row 0: Player Head, XP Bottle, Helmet, Chestplate, Leggings, Boots, Divider, Time Shower, Offhand.
 * Row 1: 9 Divider Panes.
 * Rows 2-4: Main Inventory storage (slots 9-35).
 * Row 5: Hotbar (slots 0-8).
 * 
 * Protected system items (dividers, head, XP bottle, time shower, placeholders) CANNOT be taken.
 * Normal items CAN be taken as evidence/refunds. Placing/depositing items into the GUI is strictly blocked.
 * In live mode (/inv <player>), taking items syncs directly to the online target player.
 */
public class SnapshotInspectionGui implements InventoryHolder, Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final Inventory inventory;
    private final String targetPlayerName;
    private final String snapshotId;
    private final String timestamp;
    private final boolean isLive;
    private final Player liveTarget;
    private org.bukkit.scheduler.BukkitTask liveSyncTask;

    public SnapshotInspectionGui(MoonlightVanillaSurvivalCore plugin, String targetPlayerName, String snapshotId, String timestamp,
                                 ItemStack[] storage, ItemStack[] armor, ItemStack offhand,
                                 int expLevel, int totalExp, double health, int foodLevel, String gamemode,
                                 boolean isLive, Player liveTarget) {
        this.plugin = plugin;
        this.targetPlayerName = targetPlayerName;
        this.snapshotId = snapshotId != null ? snapshotId : "live";
        this.timestamp = timestamp != null ? timestamp : "Latest";
        this.isLive = isLive;
        this.liveTarget = liveTarget;

        Component title = MiniMessage.miniMessage().deserialize("<white>" + targetPlayerName + "'s Inventory</white>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        // ---------------------------------------------------------
        // ROW 0: METADATA & EQUIPMENT (Slots 0 to 8)
        // ---------------------------------------------------------

        // Slot 0: Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetPlayerName));
        headMeta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#e879f9:#c084fc><bold>" + targetPlayerName + "</bold></gradient>"));
        List<Component> headLore = new ArrayList<>();
        headLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        headLore.add(MiniMessage.miniMessage().deserialize("<gray>Health: <red>" + String.format(Locale.US, "%.1f", health) + " / 20.0 ❤</red></gray>"));
        headLore.add(MiniMessage.miniMessage().deserialize("<gray>Food: <gold>" + foodLevel + " / 20</gold></gray>"));
        headLore.add(MiniMessage.miniMessage().deserialize("<gray>Gamemode: <white>" + gamemode + "</white></gray>"));
        if (isLive) {
            headLore.add(MiniMessage.miniMessage().deserialize("<green><bold>• Live Active Inventory</bold></green>"));
        } else {
            headLore.add(MiniMessage.miniMessage().deserialize("<gray>Snapshot: <yellow>" + this.snapshotId + "</yellow></gray>"));
        }
        headLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        headMeta.lore(headLore);
        headMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        head.setItemMeta(headMeta);
        inventory.setItem(0, head);

        // Slot 1: Experience Bottle
        ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpBottle.getItemMeta();
        xpMeta.displayName(MiniMessage.miniMessage().deserialize("<yellow>" + expLevel + " Enchantment Levels</yellow>"));
        List<Component> xpLore = new ArrayList<>();
        xpLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        xpLore.add(MiniMessage.miniMessage().deserialize("<gray>Total Experience: <white>" + totalExp + " XP</white></gray>"));
        xpLore.add(MiniMessage.miniMessage().deserialize("<blue><i>Minecraft</i></blue>"));
        xpMeta.lore(xpLore);
        xpMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        xpBottle.setItemMeta(xpMeta);
        inventory.setItem(1, xpBottle);

        // Slot 2: Helmet (armor[3])
        if (armor != null && armor.length > 3 && armor[3] != null && armor[3].getType() != Material.AIR) {
            inventory.setItem(2, armor[3].clone());
        } else {
            inventory.setItem(2, createPlaceholder("Empty Helmet Slot"));
        }

        // Slot 3: Chestplate (armor[2])
        if (armor != null && armor.length > 2 && armor[2] != null && armor[2].getType() != Material.AIR) {
            inventory.setItem(3, armor[2].clone());
        } else {
            inventory.setItem(3, createPlaceholder("Empty Chestplate Slot"));
        }

        // Slot 4: Leggings (armor[1])
        if (armor != null && armor.length > 1 && armor[1] != null && armor[1].getType() != Material.AIR) {
            inventory.setItem(4, armor[1].clone());
        } else {
            inventory.setItem(4, createPlaceholder("Empty Leggings Slot"));
        }

        // Slot 5: Boots (armor[0])
        if (armor != null && armor.length > 0 && armor[0] != null && armor[0].getType() != Material.AIR) {
            inventory.setItem(5, armor[0].clone());
        } else {
            inventory.setItem(5, createPlaceholder("Empty Boots Slot"));
        }

        // Slot 6: Empty Divider Pane
        inventory.setItem(6, createDividerPane());

        // Slot 7: Time Shower / Info Item
        ItemStack infoItem = new ItemStack(isLive ? Material.NETHER_STAR : Material.CLOCK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(MiniMessage.miniMessage().deserialize(isLive ? "<green><bold>Live Inspection</bold></green>" : "<gold><bold>Snapshot Time</bold></gold>"));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        infoLore.add(MiniMessage.miniMessage().deserialize("<gray>Timestamp: <yellow>" + this.timestamp + "</yellow></gray>"));
        if (!isLive) {
            infoLore.add(MiniMessage.miniMessage().deserialize("<gray>Snapshot ID: <yellow>" + this.snapshotId + "</yellow></gray>"));
        } else {
            infoLore.add(MiniMessage.miniMessage().deserialize("<gray>Status: <green>Online</green></gray>"));
        }
        infoLore.add(MiniMessage.miniMessage().deserialize("<green><bold>• Taking Items: ENABLED</bold></green>"));
        infoLore.add(MiniMessage.miniMessage().deserialize("<red><bold>• Placing Items: DISABLED</bold></red>"));
        infoLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        infoMeta.lore(infoLore);
        infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(7, infoItem);

        // Slot 8: Offhand / Left Hand
        if (offhand != null && offhand.getType() != Material.AIR) {
            inventory.setItem(8, offhand.clone());
        } else {
            inventory.setItem(8, createPlaceholder("Empty Offhand Slot"));
        }

        // ---------------------------------------------------------
        // ROW 1: 9 DIVIDER PANES (Slots 9 to 17)
        // ---------------------------------------------------------
        for (int i = 9; i <= 17; i++) {
            inventory.setItem(i, createDividerPane());
        }

        // ---------------------------------------------------------
        // ROWS 2-4: STORAGE CONTENTS (Slots 18 to 44)
        // Storage indices 9..35 mapped across 3 rows of 9 slots
        // ---------------------------------------------------------
        if (storage != null) {
            // Row 2: storage 9..17 -> GUI slots 18..26
            for (int i = 0; i < 9; i++) {
                int storageIdx = 9 + i;
                if (storageIdx < storage.length && storage[storageIdx] != null && storage[storageIdx].getType() != Material.AIR) {
                    inventory.setItem(18 + i, storage[storageIdx].clone());
                }
            }

            // Row 3: storage 18..26 -> GUI slots 27..35
            for (int i = 0; i < 9; i++) {
                int storageIdx = 18 + i;
                if (storageIdx < storage.length && storage[storageIdx] != null && storage[storageIdx].getType() != Material.AIR) {
                    inventory.setItem(27 + i, storage[storageIdx].clone());
                }
            }

            // Row 4: storage 27..35 -> GUI slots 36..44
            for (int i = 0; i < 9; i++) {
                int storageIdx = 27 + i;
                if (storageIdx < storage.length && storage[storageIdx] != null && storage[storageIdx].getType() != Material.AIR) {
                    inventory.setItem(36 + i, storage[storageIdx].clone());
                }
            }

            // ---------------------------------------------------------
            // ROW 5: HOTBAR CONTENTS (Slots 45 to 53)
            // Storage indices 0..8 -> GUI slots 45..53
            // ---------------------------------------------------------
            for (int i = 0; i < 9; i++) {
                if (i < storage.length && storage[i] != null && storage[i].getType() != Material.AIR) {
                    inventory.setItem(45 + i, storage[i].clone());
                }
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (this.isLive && this.liveTarget != null) {
            this.liveSyncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncLiveFromTarget, 10L, 10L);
        }
    }

    public SnapshotInspectionGui(MoonlightVanillaSurvivalCore plugin, String targetPlayerName, String snapshotId,
                                 ItemStack[] storage, ItemStack[] armor, ItemStack offhand) {
        this(plugin, targetPlayerName, snapshotId, "Recent", storage, armor, offhand, 0, 0, 20.0, 20, "SURVIVAL", false, null);
    }

    private ItemStack createDividerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack createPlaceholder(String name) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<dark_gray>" + name + "</dark_gray>"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        pane.setItemMeta(meta);
        return pane;
    }

    private boolean isProtectedItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        Material mat = item.getType();
        return mat == Material.GRAY_STAINED_GLASS_PANE ||
               mat == Material.BLACK_STAINED_GLASS_PANE ||
               mat == Material.PLAYER_HEAD ||
               mat == Material.EXPERIENCE_BOTTLE ||
               mat == Material.CLOCK ||
               mat == Material.NETHER_STAR;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player opPlayer)) {
            return;
        }

        if (!LogsCommand.isAuthorized(opPlayer)) {
            event.setCancelled(true);
            opPlayer.closeInventory();
            opPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<red>Access denied: You must be staff or OP to inspect inventories.</red>"));
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) {
            return;
        }

        // -------------------------------------------------------------
        // CLICK IN STAFF'S PERSONAL INVENTORY BELOW (rawSlot >= 54)
        // -------------------------------------------------------------
        if (rawSlot >= 54) {
            // Block shift-clicking items from staff inventory INTO the inspection GUI
            if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        // -------------------------------------------------------------
        // CLICK IN TOP INSPECTION GUI (rawSlot < 54)
        // -------------------------------------------------------------

        // 1. Permanent Protected System Slots:
        // Slot 0 (Head), Slot 1 (XP Bottle), Slot 6 (Divider), Slot 7 (Time Shower), Slots 9-17 (Divider Row)
        if (rawSlot == 0 || rawSlot == 1 || rawSlot == 6 || rawSlot == 7 || (rawSlot >= 9 && rawSlot <= 17)) {
            event.setCancelled(true);
            opPlayer.playSound(opPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            return;
        }

        ItemStack currentItem = event.getCurrentItem();

        // 2. Placeholder Check on Equipment Slots (slots 2, 3, 4, 5, 8):
        if (isProtectedItem(currentItem)) {
            event.setCancelled(true);
            opPlayer.playSound(opPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            return;
        }

        // 3. Prevent Depositing / Placing into the inspection GUI:
        if (event.getAction() == InventoryAction.PLACE_ALL ||
                event.getAction() == InventoryAction.PLACE_SOME ||
                event.getAction() == InventoryAction.PLACE_ONE ||
                event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
                event.getAction() == InventoryAction.HOTBAR_SWAP ||
                event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }

        // 4. Cursor Item Check: Cannot drop cursor item onto GUI
        if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }

        // 5. Normal Item Taking is ALLOWED:
        if (currentItem != null && currentItem.getType() != Material.AIR) {
            opPlayer.playSound(opPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);

            // If in Live mode (/inv <player>), sync removal directly to the online player
            if (isLive && liveTarget != null && liveTarget.isOnline()) {
                final int clickedSlot = rawSlot;
                Bukkit.getScheduler().runTask(plugin, () -> syncLiveRemoval(clickedSlot));
            }
        }
    }

    private void syncLiveRemoval(int slot) {
        if (liveTarget == null || !liveTarget.isOnline()) {
            return;
        }

        // Armor slots 2..5
        if (slot == 2) {
            liveTarget.getInventory().setHelmet(null);
            inventory.setItem(2, createPlaceholder("Empty Helmet Slot"));
        } else if (slot == 3) {
            liveTarget.getInventory().setChestplate(null);
            inventory.setItem(3, createPlaceholder("Empty Chestplate Slot"));
        } else if (slot == 4) {
            liveTarget.getInventory().setLeggings(null);
            inventory.setItem(4, createPlaceholder("Empty Leggings Slot"));
        } else if (slot == 5) {
            liveTarget.getInventory().setBoots(null);
            inventory.setItem(5, createPlaceholder("Empty Boots Slot"));
        } else if (slot == 8) {
            liveTarget.getInventory().setItemInOffHand(null);
            inventory.setItem(8, createPlaceholder("Empty Offhand Slot"));
        }
        // Storage slots (18..44)
        else if (slot >= 18 && slot <= 44) {
            int storageIdx = 9 + (slot - 18);
            ItemStack remaining = inventory.getItem(slot);
            liveTarget.getInventory().setItem(storageIdx, remaining != null && remaining.getType() != Material.AIR ? remaining : null);
        }
        // Hotbar slots (45..53)
        else if (slot >= 45 && slot <= 53) {
            int hotbarIdx = slot - 45;
            ItemStack remaining = inventory.getItem(slot);
            liveTarget.getInventory().setItem(hotbarIdx, remaining != null && remaining.getType() != Material.AIR ? remaining : null);
        }

        liveTarget.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this) {
            for (int slot : event.getRawSlots()) {
                if (slot < 54) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void syncLiveFromTarget() {
        if (!isLive || liveTarget == null || !liveTarget.isOnline()) {
            if (liveSyncTask != null) {
                liveSyncTask.cancel();
                liveSyncTask = null;
            }
            return;
        }
        if (inventory.getViewers().isEmpty()) {
            if (liveSyncTask != null) {
                liveSyncTask.cancel();
                liveSyncTask = null;
            }
            return;
        }

        // Equipment sync
        ItemStack[] armor = liveTarget.getInventory().getArmorContents();
        if (armor != null && armor.length > 3 && armor[3] != null && armor[3].getType() != Material.AIR) {
            inventory.setItem(2, armor[3].clone());
        } else {
            inventory.setItem(2, createPlaceholder("Empty Helmet Slot"));
        }

        if (armor != null && armor.length > 2 && armor[2] != null && armor[2].getType() != Material.AIR) {
            inventory.setItem(3, armor[2].clone());
        } else {
            inventory.setItem(3, createPlaceholder("Empty Chestplate Slot"));
        }

        if (armor != null && armor.length > 1 && armor[1] != null && armor[1].getType() != Material.AIR) {
            inventory.setItem(4, armor[1].clone());
        } else {
            inventory.setItem(4, createPlaceholder("Empty Leggings Slot"));
        }

        if (armor != null && armor.length > 0 && armor[0] != null && armor[0].getType() != Material.AIR) {
            inventory.setItem(5, armor[0].clone());
        } else {
            inventory.setItem(5, createPlaceholder("Empty Boots Slot"));
        }

        ItemStack offhand = liveTarget.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            inventory.setItem(8, offhand.clone());
        } else {
            inventory.setItem(8, createPlaceholder("Empty Offhand Slot"));
        }

        // Storage slots (18..44)
        ItemStack[] storage = liveTarget.getInventory().getStorageContents();
        for (int slot = 18; slot <= 44; slot++) {
            int storageIdx = 9 + (slot - 18);
            ItemStack targetItem = (storage != null && storageIdx < storage.length) ? storage[storageIdx] : null;
            inventory.setItem(slot, (targetItem != null && targetItem.getType() != Material.AIR) ? targetItem.clone() : null);
        }

        // Hotbar slots (45..53)
        for (int slot = 45; slot <= 53; slot++) {
            int hotbarIdx = slot - 45;
            ItemStack targetItem = (storage != null && hotbarIdx < storage.length) ? storage[hotbarIdx] : null;
            inventory.setItem(slot, (targetItem != null && targetItem.getType() != Material.AIR) ? targetItem.clone() : null);
        }

        // Update XP Bottle at Slot 1
        ItemStack xpBottle = inventory.getItem(1);
        if (xpBottle != null) {
            ItemMeta xpMeta = xpBottle.getItemMeta();
            if (xpMeta != null) {
                xpMeta.displayName(MiniMessage.miniMessage().deserialize("<yellow>" + liveTarget.getLevel() + " Enchantment Levels</yellow>"));
                xpBottle.setItemMeta(xpMeta);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            if (liveSyncTask != null) {
                liveSyncTask.cancel();
                liveSyncTask = null;
            }
            HandlerList.unregisterAll(this);
        }
    }
}
