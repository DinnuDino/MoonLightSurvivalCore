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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Streamlined Time Selection GUI.
 * Replaces clunky dial buttons with 1-click presets, direct Anvil GUI input,
 * and AuthMe-style direct chat input.
 */
public class TimeInputGui implements InventoryHolder, Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsInteractiveGui logsGui;
    private final Player staffPlayer;
    private final SubsystemCategory category;
    private final String targetPlayerName;
    private final Inventory inventory;

    public TimeInputGui(MoonlightVanillaSurvivalCore plugin, LogsInteractiveGui logsGui, Player staffPlayer, SubsystemCategory category, String targetPlayerName, Instant initialInstant) {
        this.plugin = plugin;
        this.logsGui = logsGui;
        this.staffPlayer = staffPlayer;
        this.category = category;
        this.targetPlayerName = targetPlayerName;

        Component title = MiniMessage.miniMessage().deserialize(
                "<gradient:#c084fc:#f472b6><bold>▶ SELECT TIME</bold></gradient> <dark_gray>▪</dark_gray> <gray>" + targetPlayerName + "</gray>");
        this.inventory = Bukkit.createInventory(this, 27, title);

        buildGui();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void buildGui() {
        // Aesthetic border filler
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.empty());
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);
        inventory.setItem(19, border);
        inventory.setItem(21, border);
        inventory.setItem(23, border);
        inventory.setItem(25, border);

        // Slot 4: Info Clock
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta clockMeta = clock.getItemMeta();
        clockMeta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#e879f9:#c084fc><bold>AUDIT TIME SELECTION</bold></gradient>"));
        List<Component> clockLore = new ArrayList<>();
        clockLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        clockLore.add(MiniMessage.miniMessage().deserialize("<gray>Target Player: <white>" + targetPlayerName + "</white></gray>"));
        clockLore.add(MiniMessage.miniMessage().deserialize("<gray>Category: <aqua>" + category.getDisplayName() + "</aqua></gray>"));
        clockLore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        clockLore.add(MiniMessage.miniMessage().deserialize("<gray>Select a fast 1-click preset below, or choose</gray>"));
        clockLore.add(MiniMessage.miniMessage().deserialize("<gray>to type directly via <aqua>GUI text box</aqua> or <light_purple>Chat</light_purple>.</gray>"));
        clockMeta.lore(clockLore);
        clockMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        clock.setItemMeta(clockMeta);
        inventory.setItem(4, clock);

        // Row 1: Fast 1-Click Presets (Slots 10 to 16)
        inventory.setItem(10, buildBtn(Material.NETHER_STAR, "<gradient:#a7f3d0:#34d399><bold>⚡ LATEST / NOW</bold></gradient>",
                List.of("<gray>Query live or most recent records</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(11, buildBtn(Material.FEATHER, "<gradient:#fcd34d:#f59e0b><bold>⏱ 5 MINUTES AGO</bold></gradient>",
                List.of("<gray>Quick rewind 5 minutes back</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(12, buildBtn(Material.CLOCK, "<gradient:#e879f9:#c084fc><bold>⏱ 15 MINUTES AGO</bold></gradient>",
                List.of("<gray>Quick rewind 15 minutes back</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(13, buildBtn(Material.COMPASS, "<gradient:#60a5fa:#3b82f6><bold>⏱ 1 HOUR AGO</bold></gradient>",
                List.of("<gray>Quick rewind 1 hour back</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(14, buildBtn(Material.SPYGLASS, "<gradient:#c084fc:#a855f7><bold>⏱ 6 HOURS AGO</bold></gradient>",
                List.of("<gray>Quick rewind 6 hours back</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(15, buildBtn(Material.BOOK, "<gradient:#f472b6:#ec4899><bold>⏱ 12 HOURS AGO</bold></gradient>",
                List.of("<gray>Quick rewind 12 hours back</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(16, buildBtn(Material.ENDER_EYE, "<gradient:#fb7185:#e11d48><bold>⏱ 24 HOURS AGO (1 DAY)</bold></gradient>",
                List.of("<gray>Quick rewind 24 hours back</gray>", "<green>CLICK TO SELECT</green>")));

        // Row 2: Action Controls & Direct Typable Inputs (Slots 18, 20, 22, 24, 26)
        inventory.setItem(18, buildBtn(Material.ARROW, "<gold><bold>« BACK TO MENU</bold></gold>",
                List.of("<gray>Return to Stage 3 time overview</gray>", "<yellow>CLICK TO RETURN</yellow>")));

        inventory.setItem(20, buildBtn(Material.ANVIL, "<gradient:#38bdf8:#0284c7><bold>⌨ TYPE IN GUI (ANVIL INPUT)</bold></gradient>",
                List.of("<gray>Type directly inside a GUI text box!</gray>",
                        "<gray>Supports: <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>12h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow></gray>",
                        "<aqua>CLICK TO OPEN GUI TEXTBOX</aqua>")));

        inventory.setItem(22, buildBtn(Material.RECOVERY_COMPASS, "<gradient:#a78bfa:#818cf8><bold>⏱ 7 DAYS AGO (1 WEEK)</bold></gradient>",
                List.of("<gray>Quick rewind 7 days back</gray>", "<green>CLICK TO SELECT</green>")));

        inventory.setItem(24, buildBtn(Material.WRITABLE_BOOK, "<gradient:#ec4899:#db2777><bold>💬 TYPE IN CHAT (AUTHME STYLE)</bold></gradient>",
                List.of("<gray>Prompts you in chat just like AuthMe login!</gray>",
                        "<gray>Click to auto-fill or type directly in chat</gray>",
                        "<light_purple>CLICK TO PROMPT IN CHAT</light_purple>")));

        inventory.setItem(26, buildBtn(Material.BARRIER, "<red><bold>✕ CLOSE</bold></red>",
                List.of("<gray>Exit audit inspection menu</gray>", "<red>CLICK TO CLOSE</red>")));
    }

    private ItemStack buildBtn(Material material, String nameMiniMsg, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(nameMiniMsg));
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        for (String line : loreLines) {
            lore.add(MiniMessage.miniMessage().deserialize(line));
        }
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
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

        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(staffPlayer.getUniqueId())) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot >= 27) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);

        switch (slot) {
            case 10 -> { // Latest / Now
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now(), "Latest");
            }
            case 11 -> { // 5m Ago
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(300), "5 Minutes Ago");
            }
            case 12 -> { // 15m Ago
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(900), "15 Minutes Ago");
            }
            case 13 -> { // 1h Ago
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(3600), "1 Hour Ago");
            }
            case 14 -> { // 6h Ago
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(21600), "6 Hours Ago");
            }
            case 15 -> { // 12h Ago
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(43200), "12 Hours Ago");
            }
            case 16 -> { // 24h Ago (1d)
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(86400), "24 Hours Ago");
            }
            case 18 -> { // Back
                player.closeInventory();
                logsGui.openStage3(player, category, targetPlayerName);
            }
            case 20 -> { // Direct Anvil Input GUI
                player.closeInventory();
                new AnvilTimeInput(plugin, logsGui, player, category, targetPlayerName).open();
            }
            case 22 -> { // 7d Ago
                player.closeInventory();
                logsGui.executeCategoryAction(player, category, targetPlayerName, Instant.now().minusSeconds(604800), "7 Days Ago");
            }
            case 24 -> { // Direct AuthMe-style Chat Prompt
                logsGui.promptChatTime(player, category, targetPlayerName);
            }
            case 26 -> { // Close
                player.closeInventory();
            }
            default -> {}
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == this) {
            event.setCancelled(true);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }
}
