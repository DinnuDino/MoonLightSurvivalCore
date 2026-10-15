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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Ender Chest Inspection GUI.
 * Slots 0-26: The 27 Ender Chest slots (items can be taken as evidence).
 * Slots 27-35: Decorative border and Snapshot Time Shower (strictly protected, cannot be taken).
 * Depositing/placing items into the Ender Chest is disabled.
 */
public class EnderChestInspectionGui implements InventoryHolder, Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final Inventory inventory;
    private final String targetPlayerName;
    private final String snapshotId;
    private final String timestamp;
    private final boolean isLive;
    private final Player liveTarget;
    private BukkitTask liveSyncTask;

    public EnderChestInspectionGui(MoonlightVanillaSurvivalCore plugin, String targetPlayerName, String snapshotId, String timestamp, ItemStack[] echestContents, boolean isLive, Player liveTarget) {
        this.plugin = plugin;
        this.targetPlayerName = targetPlayerName;
        this.snapshotId = snapshotId != null ? snapshotId : "live";
        this.timestamp = timestamp != null ? timestamp : "Latest";
        this.isLive = isLive;
        this.liveTarget = liveTarget;

        this.inventory = Bukkit.createInventory(this, 36, mm("<white>" + targetPlayerName + "'s Ender Chest</white>"));

        // Slots 0-26: The 27 Ender Chest slots
        if (echestContents != null) {
            for (int i = 0; i < Math.min(27, echestContents.length); i++) {
                if (echestContents[i] != null && echestContents[i].getType() != Material.AIR) {
                    inventory.setItem(i, echestContents[i].clone());
                }
            }
        }

        // Slots 27-35: Protected Decorative Border
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.empty());
        borderMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        border.setItemMeta(borderMeta);

        for (int i = 27; i < 36; i++) {
            inventory.setItem(i, border);
        }

        // Slot 31: Time Shower / Info item (Protected)
        ItemStack info = new ItemStack(isLive ? Material.NETHER_STAR : Material.CLOCK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(mm(isLive ? "<light_purple><bold>Live Ender Chest</bold></light_purple>" : "<light_purple><bold>Ender Chest Time</bold></light_purple>"));
        meta.lore(List.of(
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<gray>Player: <white>" + targetPlayerName + "</white></gray>"),
                mm("<gray>Timestamp: <yellow>" + this.timestamp + "</yellow></gray>"),
                isLive ? mm("<gray>Status: <green>Online</green></gray>") : mm("<gray>Snapshot ID: <yellow>" + this.snapshotId + "</yellow></gray>"),
                mm("<green><bold>• Taking Items: ENABLED</bold></green>"),
                mm("<red><bold>• Placing Items: DISABLED</bold></red>"),
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        info.setItemMeta(meta);
        inventory.setItem(31, info);

        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (this.isLive && this.liveTarget != null) {
            this.liveSyncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncLiveFromTarget, 10L, 10L);
        }
    }

    public EnderChestInspectionGui(MoonlightVanillaSurvivalCore plugin, String targetPlayerName, String snapshotId, String timestamp, ItemStack[] echestContents) {
        this(plugin, targetPlayerName, snapshotId, timestamp, echestContents, false, null);
    }

    private static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
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
            opPlayer.sendMessage(mm("<red>Access denied: You must be staff or OP to inspect Ender Chest logs.</red>"));
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;

        // Staff personal inventory below: block shift-clicking into Ender Chest
        if (rawSlot >= 36) {
            if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        // Protected system slots (27-35)
        if (rawSlot >= 27) {
            event.setCancelled(true);
            opPlayer.playSound(opPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
            return;
        }

        // Prevent placing/swapping into Ender Chest
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
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() != Material.AIR) {
            opPlayer.playSound(opPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);

            if (isLive && liveTarget != null && liveTarget.isOnline()) {
                final int clickedSlot = rawSlot;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (clickedSlot >= 0 && clickedSlot < 27) {
                        ItemStack remaining = inventory.getItem(clickedSlot);
                        liveTarget.getEnderChest().setItem(clickedSlot, remaining != null && remaining.getType() != Material.AIR ? remaining : null);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this && event.getRawSlots().stream().anyMatch(slot -> slot < 36)) {
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

        Inventory echest = liveTarget.getEnderChest();
        for (int i = 0; i < 27; i++) {
            ItemStack targetItem = echest.getItem(i);
            inventory.setItem(i, targetItem != null && targetItem.getType() != Material.AIR ? targetItem.clone() : null);
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
