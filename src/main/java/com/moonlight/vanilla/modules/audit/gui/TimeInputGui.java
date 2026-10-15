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

    private record TimePreset(int slot, Material material, String title, String sub, long minusSeconds, String label) {}

    private static final List<TimePreset> PRESETS = List.of(
            new TimePreset(10, Material.NETHER_STAR, "<gradient:#a7f3d0:#34d399><bold>⚡ LATEST / NOW</bold></gradient>", "<gray>Query live or most recent records</gray>", 0, "Latest"),
            new TimePreset(11, Material.FEATHER, "<gradient:#fcd34d:#f59e0b><bold>⏱ 5 MINUTES AGO</bold></gradient>", "<gray>Quick rewind 5 minutes back</gray>", 300, "5 Minutes Ago"),
            new TimePreset(12, Material.CLOCK, "<gradient:#e879f9:#c084fc><bold>⏱ 15 MINUTES AGO</bold></gradient>", "<gray>Quick rewind 15 minutes back</gray>", 900, "15 Minutes Ago"),
            new TimePreset(13, Material.COMPASS, "<gradient:#60a5fa:#3b82f6><bold>⏱ 1 HOUR AGO</bold></gradient>", "<gray>Quick rewind 1 hour back</gray>", 3600, "1 Hour Ago"),
            new TimePreset(14, Material.SPYGLASS, "<gradient:#c084fc:#a855f7><bold>⏱ 6 HOURS AGO</bold></gradient>", "<gray>Quick rewind 6 hours back</gray>", 21600, "6 Hours Ago"),
            new TimePreset(15, Material.BOOK, "<gradient:#f472b6:#ec4899><bold>⏱ 12 HOURS AGO</bold></gradient>", "<gray>Quick rewind 12 hours back</gray>", 43200, "12 Hours Ago"),
            new TimePreset(16, Material.ENDER_EYE, "<gradient:#fb7185:#e11d48><bold>⏱ 24 HOURS AGO (1 DAY)</bold></gradient>", "<gray>Quick rewind 24 hours back</gray>", 86400, "24 Hours Ago"),
            new TimePreset(22, Material.RECOVERY_COMPASS, "<gradient:#a78bfa:#818cf8><bold>⏱ 7 DAYS AGO (1 WEEK)</bold></gradient>", "<gray>Quick rewind 7 days back</gray>", 604800, "7 Days Ago")
    );

    private static final int[] BORDER_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 19, 21, 23, 25};

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

        this.inventory = Bukkit.createInventory(this, 27, mm(
                "<gradient:#c084fc:#f472b6><bold>▶ SELECT TIME</bold></gradient> <dark_gray>▪</dark_gray> <gray>" + targetPlayerName + "</gray>"));

        buildGui();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    private static ItemStack createPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    private void buildGui() {
        ItemStack filler = createPane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack border = createPane(Material.PURPLE_STAINED_GLASS_PANE);

        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);
        for (int slot : BORDER_SLOTS) inventory.setItem(slot, border);

        // Slot 4: Info Clock
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta clockMeta = clock.getItemMeta();
        clockMeta.displayName(mm("<gradient:#e879f9:#c084fc><bold>AUDIT TIME SELECTION</bold></gradient>"));
        clockMeta.lore(List.of(
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<gray>Target Player: <white>" + targetPlayerName + "</white></gray>"),
                mm("<gray>Category: <aqua>" + category.getDisplayName() + "</aqua></gray>"),
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<gray>Select a fast 1-click preset below, or choose</gray>"),
                mm("<gray>to type directly via <aqua>GUI text box</aqua> or <light_purple>Chat</light_purple>.</gray>")
        ));
        clockMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        clock.setItemMeta(clockMeta);
        inventory.setItem(4, clock);

        // Presets (Slots 10-16, 22)
        for (var preset : PRESETS) {
            inventory.setItem(preset.slot(), buildBtn(preset.material(), preset.title(),
                    List.of(preset.sub(), "<green>CLICK TO SELECT</green>")));
        }

        // Action Controls & Direct Typable Inputs (Slots 18, 20, 24, 26)
        inventory.setItem(18, buildBtn(Material.ARROW, "<gold><bold>« BACK TO MENU</bold></gold>",
                List.of("<gray>Return to Stage 3 time overview</gray>", "<yellow>CLICK TO RETURN</yellow>")));

        inventory.setItem(20, buildBtn(Material.ANVIL, "<gradient:#38bdf8:#0284c7><bold>⌨ TYPE IN GUI (ANVIL INPUT)</bold></gradient>",
                List.of("<gray>Type directly inside a GUI text box!</gray>",
                        "<gray>Supports: <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>12h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow></gray>",
                        "<aqua>CLICK TO OPEN GUI TEXTBOX</aqua>")));

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
        meta.displayName(mm(nameMiniMsg));
        List<Component> lore = new ArrayList<>();
        lore.add(mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        for (String line : loreLines) {
            lore.add(mm(line));
        }
        lore.add(mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
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

        for (var preset : PRESETS) {
            if (preset.slot() == slot) {
                player.closeInventory();
                Instant targetTime = preset.minusSeconds() == 0 ? Instant.now() : Instant.now().minusSeconds(preset.minusSeconds());
                logsGui.executeCategoryAction(player, category, targetPlayerName, targetTime, preset.label());
                return;
            }
        }

        switch (slot) {
            case 18 -> { player.closeInventory(); logsGui.openStage3(player, category, targetPlayerName); }
            case 20 -> { player.closeInventory(); new AnvilTimeInput(plugin, logsGui, player, category, targetPlayerName).open(); }
            case 24 -> logsGui.promptChatTime(player, category, targetPlayerName);
            case 26 -> player.closeInventory();
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
