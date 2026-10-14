package com.moonlight.vanilla.modules.gui;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.EnderChestInspectionGui;
import com.moonlight.vanilla.modules.audit.gui.LogsInteractiveGui;
import com.moonlight.vanilla.modules.audit.gui.LogsListGui;
import com.moonlight.vanilla.modules.audit.gui.SnapshotInspectionGui;
import com.moonlight.vanilla.modules.audit.gui.TimeInputGui;
import com.moonlight.vanilla.modules.ranks.RanksInventoryHolder;
import com.moonlight.vanilla.modules.wraps.WrapInventoryHolder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * GuiSecurityListener
 * Comprehensive anti-theft, anti-exploit, and inventory desync protection across
 * ALL Moonlight GUIs (/itemwarps, /ranks, /logs, /inv, /ec).
 * 
 * Prevents rapid-click ghost items, hotbar swap glitching, shift-click leaks,
 * and desync drops on inventory close.
 */
public class GuiSecurityListener implements Listener {

    private static MoonlightVanillaSurvivalCore plugin;
    public static final NamespacedKey GUI_ITEM_KEY = new NamespacedKey("moonlight", "gui_item");
    private static final NamespacedKey ACTION_KEY = new NamespacedKey("moonlight", "gui_action");
    private static final NamespacedKey WRAP_KEY = new NamespacedKey("moonlight", "gui_wrap_key");
    private static final NamespacedKey PAGE_KEY = new NamespacedKey("moonlight", "gui_page_target");

    public GuiSecurityListener(MoonlightVanillaSurvivalCore pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Stamped onto all GUI items during creation.
     */
    public static ItemStack markGuiItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(GUI_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Determines whether an ItemStack originates from any Moonlight GUI.
     */
    public static boolean isGuiItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(GUI_ITEM_KEY, PersistentDataType.BYTE)) return true;
        if (pdc.has(ACTION_KEY, PersistentDataType.STRING)) return true;
        if (pdc.has(WRAP_KEY, PersistentDataType.STRING)) return true;
        if (pdc.has(PAGE_KEY, PersistentDataType.INTEGER)) return true;

        if (meta.hasDisplayName()) {
            String plain = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            if (plain.contains("Default Vanilla Item")
                    || plain.contains("Default Vanilla Appearance")
                    || plain.contains("MoonLight Weapon Wraps")
                    || plain.contains("← Previous Page")
                    || plain.contains("Next Page →")
                    || plain.contains("Close Menu")
                    || plain.contains("✦ MoonLight ❙ Vanilla Item ✦")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an inventory is an interactive Moonlight GUI.
     */
    public static boolean isMoonlightGui(Inventory inv) {
        if (inv == null) return false;
        InventoryHolder holder = inv.getHolder();
        return holder instanceof WrapInventoryHolder
                || holder instanceof RanksInventoryHolder
                || holder instanceof TimeInputGui
                || holder instanceof LogsListGui
                || holder instanceof LogsInteractiveGui.LogsGuiHolder;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        boolean inMoonlightGui = isMoonlightGui(top);

        if (inMoonlightGui) {
            // Cancel and deny every single movement event while inside a Moonlight GUI
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);

            if (event.getWhoClicked() instanceof Player player) {
                // Wipe cursor immediately to prevent client desync leak
                player.setItemOnCursor(null);
                event.getView().setCursor(null);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }

            // Block any hotkey or shift movement from the bottom inventory
            if (event.getClick().isShiftClick()
                    || event.getClick().isKeyboardClick()
                    || event.getAction() == InventoryAction.HOTBAR_SWAP
                    || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
                return;
            }
        }

        // Global safeguard: if ANY GUI item is being clicked or moved, delete it immediately
        if (isGuiItem(event.getCurrentItem()) || isGuiItem(event.getCursor())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if (event.getCurrentItem() != null && isGuiItem(event.getCurrentItem())) {
                event.getCurrentItem().setAmount(0);
            }
            if (event.getCursor() != null && isGuiItem(event.getCursor())) {
                event.getView().setCursor(null);
            }
            if (event.getWhoClicked() instanceof Player player) {
                player.setItemOnCursor(null);
                cleanGuiItemsFromPlayer(player);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isMoonlightGui(event.getView().getTopInventory())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if (event.getWhoClicked() instanceof Player player) {
                player.setItemOnCursor(null);
                event.getView().setCursor(null);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (isMoonlightGui(event.getInventory())) {
            if (event.getPlayer() instanceof Player player) {
                // Instantly wipe cursor item so Paper doesn't drop it into the world or inventory
                player.setItemOnCursor(null);
                event.getView().setCursor(null);

                // Deep scrub immediately
                cleanGuiItemsFromPlayer(player);
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);

                // Additional 1-tick delayed scrub to defeat quick-close packet exploits
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        cleanGuiItemsFromPlayer(player);
                        player.updateInventory();
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (isGuiItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getItemDrop().remove();
            cleanGuiItemsFromPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (isGuiItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isGuiItem(event.getItem())) {
            event.setCancelled(true);
            if (event.getItem() != null) {
                event.getItem().setAmount(0);
            }
            cleanGuiItemsFromPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        if (isGuiItem(event.getMainHandItem()) || isGuiItem(event.getOffHandItem())) {
            event.setCancelled(true);
            cleanGuiItemsFromPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        cleanGuiItemsFromPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cleanGuiItemsFromPlayer(event.getPlayer());
    }

    /**
     * Erases any GUI button item from a player's inventory, cursor, and offhand.
     */
    public static void cleanGuiItemsFromPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        boolean removed = false;

        // Check cursor
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && isGuiItem(cursor)) {
            player.setItemOnCursor(null);
            removed = true;
        }

        // Check inventory contents
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && isGuiItem(item)) {
                player.getInventory().setItem(i, null);
                removed = true;
            }
        }

        // Check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && isGuiItem(offhand)) {
            player.getInventory().setItemInOffHand(null);
            removed = true;
        }

        if (removed) {
            player.updateInventory();
        }
    }
}
