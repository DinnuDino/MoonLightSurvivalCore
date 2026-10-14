package com.moonlight.vanilla.modules.audit.gui;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.LogsInteractiveGui.SubsystemCategory;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Log Results Container GUI.
 * Displays queried item events, block/action logs, position breadcrumbs, and sessions in a paginated RPG container.
 */
public class LogsListGui implements InventoryHolder, Listener {

    public record LogDisplayEntry(Material icon, String title, List<String> details) {}

    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsInteractiveGui logsGui;
    private final SubsystemCategory category;
    private final String targetPlayerName;
    private final String displayTime;
    private final List<LogDisplayEntry> entries;
    private final int page;
    private final Inventory inventory;

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public LogsListGui(MoonlightVanillaSurvivalCore plugin, LogsInteractiveGui logsGui, SubsystemCategory category,
                       String targetPlayerName, String displayTime, List<LogDisplayEntry> entries, int page) {
        this.plugin = plugin;
        this.logsGui = logsGui;
        this.category = category;
        this.targetPlayerName = targetPlayerName;
        this.displayTime = displayTime;
        this.entries = entries != null ? entries : List.of();
        this.page = Math.max(0, page);

        Component title = MiniMessage.miniMessage().deserialize(
                "<gradient:#c084fc:#f472b6><bold>▶ LOGS</bold></gradient> <dark_gray>▪</dark_gray> <gray>" + targetPlayerName + " (" + category.getDisplayName() + ")</gray>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        buildGui();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void buildGui() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        ItemStack border = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.empty());
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        int[] lateral = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int s : lateral) {
            inventory.setItem(s, border);
        }

        // Header info at Slot 4
        ItemStack header = new ItemStack(category.getIcon());
        ItemMeta hMeta = header.getItemMeta();
        hMeta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#e879f9:#c084fc><bold>" + category.getDisplayName().toUpperCase() + " AUDIT</bold></gradient>"));
        List<Component> hLore = new ArrayList<>();
        hLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        hLore.add(MiniMessage.miniMessage().deserialize("<gray>Target Player: <white>" + targetPlayerName + "</white></gray>"));
        hLore.add(MiniMessage.miniMessage().deserialize("<gray>Time Filter: <yellow>≤ " + displayTime + "</yellow></gray>"));
        hLore.add(MiniMessage.miniMessage().deserialize("<gray>Records Found: <gold>" + entries.size() + "</gold></gray>"));
        hLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        hMeta.lore(hLore);
        hMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        header.setItemMeta(hMeta);
        inventory.setItem(4, header);

        // Paginate entries
        int pageSize = SLOTS.length; // 28
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / pageSize));
        int safePage = Math.min(page, totalPages - 1);

        int start = safePage * pageSize;
        int end = Math.min(start + pageSize, entries.size());

        for (int i = start; i < end; i++) {
            LogDisplayEntry entry = entries.get(i);
            int slot = SLOTS[i - start];
            inventory.setItem(slot, buildEntryItem(entry));
        }

        // Slot 45: Back to Stage 3
        inventory.setItem(45, buildNavBtn(Material.ARROW, "<gold><bold>BACK TO TIME MENU</bold></gold>", "Return to Stage 3"));

        // Slot 48 / 50: Pagination
        if (safePage > 0) {
            inventory.setItem(48, buildNavBtn(Material.ARROW, "<aqua><bold>PREVIOUS PAGE</bold></aqua>", "Page " + safePage + " of " + totalPages));
        }
        if (safePage < totalPages - 1) {
            inventory.setItem(50, buildNavBtn(Material.ARROW, "<aqua><bold>NEXT PAGE</bold></aqua>", "Page " + (safePage + 2) + " of " + totalPages));
        }

        // Slot 49: Enter New Time (Hub)
        inventory.setItem(49, buildNavBtn(Material.CLOCK, "<gradient:#e879f9:#c084fc><bold>ENTER NEW TIME</bold></gradient>", "Choose preset or type time directly"));

        // Slot 53: Close
        inventory.setItem(53, buildNavBtn(Material.BARRIER, "<red><bold>CLOSE MENU</bold></red>", "Exit"));
    }

    private ItemStack buildEntryItem(LogDisplayEntry entry) {
        Material mat = entry.icon() != null && entry.icon() != Material.AIR ? entry.icon() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(entry.title()));
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        for (String line : entry.details()) {
            lore.add(MiniMessage.miniMessage().deserialize(line));
        }
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavBtn(Material mat, String name, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().deserialize("<gray>" + desc + "</gray>"));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);

        int pageSize = SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / pageSize));

        if (slot == 45) { // Back
            player.closeInventory();
            logsGui.openStage3(player, category, targetPlayerName);
        } else if (slot == 48 && page > 0) { // Prev
            player.closeInventory();
            new LogsListGui(plugin, logsGui, category, targetPlayerName, displayTime, entries, page - 1).open(player);
        } else if (slot == 50 && page < totalPages - 1) { // Next
            player.closeInventory();
            new LogsListGui(plugin, logsGui, category, targetPlayerName, displayTime, entries, page + 1).open(player);
        } else if (slot == 49) { // Dial new time
            player.closeInventory();
            new TimeInputGui(plugin, logsGui, player, category, targetPlayerName, null).open(player);
        } else if (slot == 53) { // Close
            player.closeInventory();
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }
}
