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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static final String[] ARMOR_NAMES = {
            "Empty Helmet Slot", "Empty Chestplate Slot", "Empty Leggings Slot", "Empty Boots Slot"
    };

    private static final Set<Material> PROTECTED_MATERIALS = Set.of(
            Material.GRAY_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE,
            Material.PLAYER_HEAD, Material.EXPERIENCE_BOTTLE,
            Material.CLOCK, Material.NETHER_STAR
    );

    private final MoonlightVanillaSurvivalCore plugin;
    private final Inventory inventory;
    private final String targetPlayerName;
    private final String snapshotId;
    private final String timestamp;
    private final boolean isLive;
    private final Player liveTarget;
    private BukkitTask liveSyncTask;

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

        this.inventory = Bukkit.createInventory(this, 54, mm("<white>" + targetPlayerName + "'s Inventory</white>"));

        // Slot 0: Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetPlayerName));
        headMeta.displayName(mm("<gradient:#e879f9:#c084fc><bold>" + targetPlayerName + "</bold></gradient>"));
        List<Component> headLore = new ArrayList<>();
        headLore.add(mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        headLore.add(mm("<gray>Health: <red>" + String.format(Locale.US, "%.1f", health) + " / 20.0 ❤</red></gray>"));
        headLore.add(mm("<gray>Food: <gold>" + foodLevel + " / 20</gold></gray>"));
        headLore.add(mm("<gray>Gamemode: <white>" + gamemode + "</white></gray>"));
        headLore.add(isLive ? mm("<green><bold>• Live Active Inventory</bold></green>") : mm("<gray>Snapshot: <yellow>" + this.snapshotId + "</yellow></gray>"));
        headLore.add(mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        headMeta.lore(headLore);
        headMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        head.setItemMeta(headMeta);
        inventory.setItem(0, head);

        // Slot 1: Experience Bottle
        ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpBottle.getItemMeta();
        xpMeta.displayName(mm("<yellow>" + expLevel + " Enchantment Levels</yellow>"));
        xpMeta.lore(List.of(
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<gray>Total Experience: <white>" + totalExp + " XP</white></gray>"),
                mm("<blue><i>Minecraft</i></blue>")
        ));
        xpMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        xpBottle.setItemMeta(xpMeta);
        inventory.setItem(1, xpBottle);

        // Slots 2-5: Armor (Helmet, Chestplate, Leggings, Boots)
        for (int i = 0; i < 4; i++) {
            int armorIdx = 3 - i;
            ItemStack piece = (armor != null && armor.length > armorIdx) ? armor[armorIdx] : null;
            setEquipment(2 + i, piece, ARMOR_NAMES[i]);
        }

        // Slot 6: Empty Divider Pane
        inventory.setItem(6, createDividerPane());

        // Slot 7: Time Shower / Info Item
        ItemStack infoItem = new ItemStack(isLive ? Material.NETHER_STAR : Material.CLOCK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(mm(isLive ? "<green><bold>Live Inspection</bold></green>" : "<gold><bold>Snapshot Time</bold></gold>"));
        infoMeta.lore(List.of(
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<gray>Timestamp: <yellow>" + this.timestamp + "</yellow></gray>"),
                isLive ? mm("<gray>Status: <green>Online</green></gray>") : mm("<gray>Snapshot ID: <yellow>" + this.snapshotId + "</yellow></gray>"),
                mm("<green><bold>• Taking Items: ENABLED</bold></green>"),
                mm("<red><bold>• Placing Items: DISABLED</bold></red>"),
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>")
        ));
        infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(7, infoItem);

        // Slot 8: Offhand
        setEquipment(8, offhand, "Empty Offhand Slot");

        // Row 1: 9 Divider Panes (Slots 9 to 17)
        for (int i = 9; i <= 17; i++) {
            inventory.setItem(i, createDividerPane());
        }

        // Rows 2-4: Storage (Slots 18 to 44) & Row 5: Hotbar (Slots 45 to 53)
        if (storage != null) {
            for (int i = 0; i < 27; i++) {
                int storageIdx = 9 + i;
                if (storageIdx < storage.length && storage[storageIdx] != null && storage[storageIdx].getType() != Material.AIR) {
                    inventory.setItem(18 + i, storage[storageIdx].clone());
                }
            }
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

    private static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    private void setEquipment(int slot, ItemStack item, String placeholderName) {
        if (item != null && item.getType() != Material.AIR) {
            inventory.setItem(slot, item.clone());
        } else {
            inventory.setItem(slot, createPlaceholder(placeholderName));
        }
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
        meta.displayName(mm("<dark_gray>" + name + "</dark_gray>"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        pane.setItemMeta(meta);
        return pane;
    }

    private boolean isProtectedItem(ItemStack item) {
        return item != null && PROTECTED_MATERIALS.contains(item.getType());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getWhoClicked() instanceof Player opPlayer)) return;

        if (!LogsCommand.isAuthorized(opPlayer)) {
            event.setCancelled(true);
            opPlayer.closeInventory();
            opPlayer.sendMessage(mm("<red>Access denied: You must be staff or OP to inspect inventories.</red>"));
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;

        // Staff personal inventory below: block shift-clicking into GUI
        if (rawSlot >= 54) {
            if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        // Permanent Protected System Slots (0, 1, 6, 7, 9-17)
        if (rawSlot == 0 || rawSlot == 1 || rawSlot == 6 || rawSlot == 7 || (rawSlot >= 9 && rawSlot <= 17)) {
            event.setCancelled(true);
            opPlayer.playSound(opPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            return;
        }

        ItemStack currentItem = event.getCurrentItem();

        // Protected item / placeholder in equipment slots (2, 3, 4, 5, 8)
        if (isProtectedItem(currentItem)) {
            event.setCancelled(true);
            opPlayer.playSound(opPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            return;
        }

        // Prevent placing into inspection GUI
        if (event.getAction() == InventoryAction.PLACE_ALL ||
                event.getAction() == InventoryAction.PLACE_SOME ||
                event.getAction() == InventoryAction.PLACE_ONE ||
                event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
                event.getAction() == InventoryAction.HOTBAR_SWAP ||
                event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }

        // Prevent dropping cursor item
        if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }

        // Taking items is allowed
        if (currentItem != null && currentItem.getType() != Material.AIR) {
            opPlayer.playSound(opPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);

            if (isLive && liveTarget != null && liveTarget.isOnline()) {
                final int clickedSlot = rawSlot;
                Bukkit.getScheduler().runTask(plugin, () -> syncLiveRemoval(clickedSlot));
            }
        }
    }

    private void syncLiveRemoval(int slot) {
        if (liveTarget == null || !liveTarget.isOnline()) return;

        var targetInv = liveTarget.getInventory();
        switch (slot) {
            case 2 -> { targetInv.setHelmet(null); inventory.setItem(2, createPlaceholder(ARMOR_NAMES[0])); }
            case 3 -> { targetInv.setChestplate(null); inventory.setItem(3, createPlaceholder(ARMOR_NAMES[1])); }
            case 4 -> { targetInv.setLeggings(null); inventory.setItem(4, createPlaceholder(ARMOR_NAMES[2])); }
            case 5 -> { targetInv.setBoots(null); inventory.setItem(5, createPlaceholder(ARMOR_NAMES[3])); }
            case 8 -> { targetInv.setItemInOffHand(null); inventory.setItem(8, createPlaceholder("Empty Offhand Slot")); }
            default -> {
                if (slot >= 18 && slot <= 44) {
                    ItemStack remaining = inventory.getItem(slot);
                    targetInv.setItem(9 + (slot - 18), remaining != null && remaining.getType() != Material.AIR ? remaining : null);
                } else if (slot >= 45 && slot <= 53) {
                    ItemStack remaining = inventory.getItem(slot);
                    targetInv.setItem(slot - 45, remaining != null && remaining.getType() != Material.AIR ? remaining : null);
                }
            }
        }
        liveTarget.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this && event.getRawSlots().stream().anyMatch(slot -> slot < 54)) {
            event.setCancelled(true);
        }
    }

    private void syncLiveFromTarget() {
        if (!isLive || liveTarget == null || !liveTarget.isOnline() || inventory.getViewers().isEmpty()) {
            if (liveSyncTask != null) {
                liveSyncTask.cancel();
                liveSyncTask = null;
            }
            return;
        }

        // Equipment sync
        ItemStack[] armor = liveTarget.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            int armorIdx = 3 - i;
            ItemStack piece = (armor != null && armor.length > armorIdx) ? armor[armorIdx] : null;
            setEquipment(2 + i, piece, ARMOR_NAMES[i]);
        }
        setEquipment(8, liveTarget.getInventory().getItemInOffHand(), "Empty Offhand Slot");

        // Storage & Hotbar sync
        ItemStack[] storage = liveTarget.getInventory().getStorageContents();
        for (int i = 0; i < 27; i++) {
            int storageIdx = 9 + i;
            ItemStack targetItem = (storage != null && storageIdx < storage.length) ? storage[storageIdx] : null;
            inventory.setItem(18 + i, targetItem != null && targetItem.getType() != Material.AIR ? targetItem.clone() : null);
        }
        for (int i = 0; i < 9; i++) {
            ItemStack targetItem = (storage != null && i < storage.length) ? storage[i] : null;
            inventory.setItem(45 + i, targetItem != null && targetItem.getType() != Material.AIR ? targetItem.clone() : null);
        }

        // XP Bottle sync
        ItemStack xpBottle = inventory.getItem(1);
        if (xpBottle != null) {
            ItemMeta xpMeta = xpBottle.getItemMeta();
            if (xpMeta != null) {
                xpMeta.displayName(mm("<yellow>" + liveTarget.getLevel() + " Enchantment Levels</yellow>"));
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
