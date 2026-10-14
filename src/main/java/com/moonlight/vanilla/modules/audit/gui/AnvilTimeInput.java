package com.moonlight.vanilla.modules.audit.gui;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.LogsInteractiveGui.SubsystemCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct typable Anvil GUI for audit logs time & duration input.
 * Allows staff to type directly on screen (e.g. 10m, 1h, 12h, 1d, 15:30:00).
 */
public class AnvilTimeInput implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsInteractiveGui logsGui;
    private final Player staffPlayer;
    private final SubsystemCategory category;
    private final String targetPlayerName;

    private boolean closedByConfirm = false;

    public AnvilTimeInput(MoonlightVanillaSurvivalCore plugin, LogsInteractiveGui logsGui, Player staffPlayer, SubsystemCategory category, String targetPlayerName) {
        this.plugin = plugin;
        this.logsGui = logsGui;
        this.staffPlayer = staffPlayer;
        this.category = category;
        this.targetPlayerName = targetPlayerName;
    }

    public void open() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        InventoryView view = staffPlayer.openAnvil(null, true);
        if (view == null) {
            // Fallback to AuthMe-style chat prompt if anvil could not be opened
            HandlerList.unregisterAll(this);
            logsGui.promptChatTime(staffPlayer, category, targetPlayerName);
            return;
        }

        Inventory top = view.getTopInventory();
        ItemStack inputItem = new ItemStack(Material.PAPER);
        ItemMeta meta = inputItem.getItemMeta();
        meta.displayName(Component.text("10m"));
        inputItem.setItemMeta(meta);
        top.setItem(0, inputItem);

        staffPlayer.playSound(staffPlayer.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView() instanceof AnvilView view)) return;
        if (!view.getPlayer().getUniqueId().equals(staffPlayer.getUniqueId())) return;

        String renameText = view.getRenameText();
        String candidate = (renameText != null && !renameText.isBlank()) ? renameText.trim() : "10m";

        ItemStack result = new ItemStack(Material.EMERALD);
        ItemMeta meta = result.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(
                "<green><bold>✔ CONFIRM: </bold><yellow>" + candidate + "</yellow></green>"));
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to search audit records for this time.</gray>"));
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>Examples: 5m, 10m, 1h, 12h, 1d, 15:30:00</dark_gray>"));
        meta.lore(lore);
        result.setItemMeta(meta);

        event.setResult(result);
        view.setRepairCost(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!event.getWhoClicked().getUniqueId().equals(staffPlayer.getUniqueId())) return;
        if (!(event.getView() instanceof AnvilView view)) return;

        int rawSlot = event.getRawSlot();

        // Protect anvil input slots
        if (rawSlot == 0 || rawSlot == 1) {
            event.setCancelled(true);
            return;
        }

        // Prevent shift clicking into top inventory
        if (event.isShiftClick() && rawSlot >= 3) {
            event.setCancelled(true);
            return;
        }

        // Slot 2: Result confirmation slot
        if (rawSlot == 2) {
            event.setCancelled(true);
            String text = view.getRenameText();
            if (text == null || text.isBlank()) {
                ItemStack slot0 = event.getInventory().getItem(0);
                if (slot0 != null && slot0.hasItemMeta() && slot0.getItemMeta().hasDisplayName()) {
                    text = PlainTextComponentSerializer.plainText().serialize(slot0.getItemMeta().displayName());
                }
            }
            if (text == null || text.isBlank()) {
                text = "10m";
            }

            Instant instant = logsGui.getLogsCommand().parseRelativeOrAbsoluteTime(text);
            if (instant == null) {
                staffPlayer.playSound(staffPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                staffPlayer.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Invalid time format <yellow>'" + text + "'</yellow>! Use e.g. <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, or <yellow>15:30:00</yellow>.</red>"));
                return;
            }

            closedByConfirm = true;
            staffPlayer.playSound(staffPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
            staffPlayer.closeInventory();

            final String displayText = text;
            final Instant finalInstant = instant;
            Bukkit.getScheduler().runTask(plugin, () -> {
                logsGui.executeCategoryAction(staffPlayer, category, targetPlayerName, finalInstant, displayText);
            });
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(staffPlayer.getUniqueId())) return;
        if (!(event.getView() instanceof AnvilView)) return;

        event.getView().getTopInventory().clear();
        HandlerList.unregisterAll(this);

        if (!closedByConfirm) {
            // Player closed without confirming; reopen Stage 3 smoothly
            Bukkit.getScheduler().runTask(plugin, () -> logsGui.openStage3(staffPlayer, category, targetPlayerName));
        }
    }
}
