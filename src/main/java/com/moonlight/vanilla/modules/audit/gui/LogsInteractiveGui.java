package com.moonlight.vanilla.modules.audit.gui;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.commands.LogsCommand;
import com.moonlight.vanilla.utils.VanishUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LogsInteractiveGui
 * 4-Stage interactive GUI for staff audit inspection:
 * Stage 1: Category selection
 * Stage 2: Target player head browser (with custom username chat input)
 * Stage 3: Point-in-time rewind presets & direct input
 * Stage 4: Database snapshot archive paginator
 */
public class LogsInteractiveGui implements Listener {

    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsCommand logsCommand;
    private final Map<UUID, ChatPrompt> activeChatPrompts = new ConcurrentHashMap<>();

    private static final int[] USABLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public LogsInteractiveGui(MoonlightVanillaSurvivalCore plugin, LogsCommand logsCommand) {
        this.plugin = plugin;
        this.logsCommand = logsCommand;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public LogsCommand getLogsCommand() {
        return logsCommand;
    }

    private static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    public void openStage1(Player player) {
        LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_1_CATEGORY, null, null, 0);
        Inventory inv = Bukkit.createInventory(holder, 54, mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <dark_gray>▪</dark_gray> <gray>Select Category</gray>"));
        holder.setInventory(inv);
        applyDualToneBorder(inv);

        inv.setItem(11, buildCategoryItem(SubsystemCategory.INVENTORY));
        inv.setItem(13, buildCategoryItem(SubsystemCategory.ENDER_CHEST));
        inv.setItem(15, buildCategoryItem(SubsystemCategory.POSITION));
        inv.setItem(29, buildCategoryItem(SubsystemCategory.ITEM_TRANSACTIONS));
        inv.setItem(31, buildCategoryItem(SubsystemCategory.BLOCK_ACTIONS));
        inv.setItem(33, buildCategoryItem(SubsystemCategory.SESSIONS));
        inv.setItem(49, buildUtilityItem(Material.BARRIER, "<red><bold>CLOSE MENU</bold></red>", "Exit Audit System", List.of("Close the interactive audit menu."), "CLICK TO CLOSE"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.1f);
    }

    public void openStage2(Player player, SubsystemCategory category, int page) {
        LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_2_PLAYER, category, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <dark_gray>▪</dark_gray> <gray>Select Target Player</gray>"));
        holder.setInventory(inv);
        applyDualToneBorder(inv);

        List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> VanishUtil.canSee(player, p))
                .map(p -> (Player) p)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int pageSize = USABLE_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / pageSize));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        int startIndex = safePage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, onlinePlayers.size());

        for (int i = startIndex; i < endIndex; ++i) {
            inv.setItem(USABLE_SLOTS[i - startIndex], buildPlayerSkull(onlinePlayers.get(i)));
        }

        inv.setItem(45, buildUtilityItem(Material.ARROW, "<gold><bold>BACK TO CATEGORIES</bold></gold>", "Category Selection", List.of("Return to Stage 1 category menu."), "CLICK TO RETURN"));
        inv.setItem(49, buildUtilityItem(Material.NAME_TAG, "<gradient:#e879f9:#c084fc><bold>TYPE CUSTOM / OFFLINE USERNAME</bold></gradient>", "Custom Player Target", List.of("Close inventory and type any offline", "or historical username via server chat."), "CLICK TO ENTER CHAT INPUT"));
        if (safePage > 0) {
            inv.setItem(48, buildUtilityItem(Material.ARROW, "<aqua><bold>PREVIOUS PAGE</bold></aqua>", "Page " + safePage + " of " + totalPages, List.of("View previous players."), "CLICK TO VIEW"));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(50, buildUtilityItem(Material.ARROW, "<aqua><bold>NEXT PAGE</bold></aqua>", "Page " + (safePage + 2) + " of " + totalPages, List.of("View next players."), "CLICK TO VIEW"));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
    }

    public void openStage3(Player player, SubsystemCategory category, String targetPlayerName) {
        LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_3_TIME, category, targetPlayerName, 0);
        Inventory inv = Bukkit.createInventory(holder, 54, mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <dark_gray>▪</dark_gray> <gray>" + targetPlayerName + "</gray>"));
        holder.setInventory(inv);
        applyDualToneBorder(inv);

        Player onlineTarget = Bukkit.getPlayerExact(targetPlayerName);
        boolean isOnline = onlineTarget != null && onlineTarget.isOnline();
        boolean isLiveEligible = (category == SubsystemCategory.INVENTORY || category == SubsystemCategory.ENDER_CHEST) && isOnline;

        inv.setItem(13, buildUtilityItem(Material.NETHER_STAR,
                isLiveEligible ? "<gradient:#a7f3d0:#34d399><bold>LIVE ACTIVE INSPECTION</bold></gradient>" : "<gradient:#e879f9:#c084fc><bold>LATEST LOG RECORD</bold></gradient>",
                isLiveEligible ? "Real-time 10-Tick Sync" : "Most Recent Database Record",
                List.of(isLiveEligible ? "Opens real-time active inspection with" : "Queries the most recent record",
                        isLiveEligible ? "live synchronization from " + targetPlayerName + "." : "saved in database for " + targetPlayerName + "."),
                "CLICK TO SELECT"));

        inv.setItem(20, buildUtilityItem(Material.CLOCK, "<gradient:#e879f9:#c084fc><bold>10 MINUTES AGO</bold></gradient>", "Quick Rewind (-10m)", List.of("Inspect audit state 10 minutes", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(21, buildUtilityItem(Material.COMPASS, "<gradient:#e879f9:#c084fc><bold>1 HOUR AGO</bold></gradient>", "Quick Rewind (-1h)", List.of("Inspect audit state 1 hour", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(22, buildUtilityItem(Material.SPYGLASS, "<gradient:#e879f9:#c084fc><bold>12 HOURS AGO</bold></gradient>", "Quick Rewind (-12h)", List.of("Inspect audit state 12 hours", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(23, buildUtilityItem(Material.BOOK, "<gradient:#e879f9:#c084fc><bold>24 HOURS AGO (1 DAY)</bold></gradient>", "Quick Rewind (-24h)", List.of("Inspect audit state 24 hours", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(24, buildUtilityItem(Material.ANVIL, "<gradient:#38bdf8:#0284c7><bold>⌨ ENTER TIME DIRECTLY (GUI)</bold></gradient>", "Direct Anvil Input & Presets", List.of("Type exact time or duration (e.g. 10m, 1h,", "12h, 1d, 15:30:00) directly on screen."), "CLICK TO OPEN TIME SELECTOR"));

        if (category == SubsystemCategory.INVENTORY || category == SubsystemCategory.ENDER_CHEST) {
            inv.setItem(31, buildUtilityItem(Material.CHEST_MINECART, "<gradient:#e879f9:#c084fc><bold>BROWSE SNAPSHOT HISTORY</bold></gradient>", "Database Snapshot Archive", List.of("Opens a paginated list of actual database", "snapshot rows recorded for " + targetPlayerName + "."), "CLICK TO BROWSE"));
        }

        inv.setItem(45, buildUtilityItem(Material.ARROW, "<gold><bold>BACK TO PLAYERS</bold></gold>", "Player Selection", List.of("Return to target player selector."), "CLICK TO RETURN"));
        inv.setItem(49, buildUtilityItem(Material.WRITABLE_BOOK, "<gradient:#ec4899:#db2777><bold>💬 TYPE IN CHAT (AUTHME STYLE)</bold></gradient>", "Direct Chat Prompt & Auto-Fill", List.of("Prompts you in chat just like AuthMe login!", "Type 10m, 1h, 1d, 15:30:00, or click", "interactive chat button to auto-fill."), "CLICK TO PROMPT IN CHAT"));
        inv.setItem(53, buildUtilityItem(Material.BARRIER, "<red><bold>CLOSE MENU</bold></red>", "Exit", List.of("Close audit inspection menu."), "CLICK TO CLOSE"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.3f);
    }

    public void openStage4SnapshotHistory(Player player, SubsystemCategory category, String targetPlayerName, int page) {
        player.sendMessage(mm("<gray>Querying snapshot database archives for <white>" + targetPlayerName + "</white>...</gray>"));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<SnapshotRow> snapshots = new ArrayList<>();
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                String sql = "SELECT snapshot_id, timestamp, snapshot_reason, world_name, x, y, z, duration_seconds FROM player_inventory_snapshots WHERE username = ? ORDER BY id DESC LIMIT 28 OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetPlayerName);
                    ps.setInt(2, page * 28);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            snapshots.add(new SnapshotRow(rs.getString("snapshot_id"), rs.getString("timestamp"), rs.getString("snapshot_reason"), rs.getString("world_name"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getInt("duration_seconds")));
                        }
                    }
                }
            } catch (Exception e) {
                player.sendMessage(mm("<red>Database error loading snapshots: " + e.getMessage() + "</red>"));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_4_SNAPSHOT_HISTORY, category, targetPlayerName, page);
                Inventory inv = Bukkit.createInventory(holder, 54, mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <dark_gray>▪</dark_gray> <gray>" + targetPlayerName + " Archive</gray>"));
                holder.setInventory(inv);
                applyDualToneBorder(inv);

                for (int i = 0; i < snapshots.size(); ++i) {
                    inv.setItem(USABLE_SLOTS[i], buildSnapshotItem(snapshots.get(i)));
                }

                inv.setItem(45, buildUtilityItem(Material.ARROW, "<gold><bold>BACK TO TIME OPTIONS</bold></gold>", "Time Selection", List.of("Return to snapshot time options."), "CLICK TO RETURN"));
                if (page > 0) {
                    inv.setItem(48, buildUtilityItem(Material.ARROW, "<aqua><bold>PREVIOUS PAGE</bold></aqua>", "Page " + page, List.of("View previous snapshots."), "CLICK TO VIEW"));
                }
                if (snapshots.size() == 28) {
                    inv.setItem(50, buildUtilityItem(Material.ARROW, "<aqua><bold>NEXT PAGE</bold></aqua>", "Page " + (page + 2), List.of("View older snapshots."), "CLICK TO VIEW"));
                }
                player.openInventory(inv);
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LogsGuiHolder holder)) return;
        if (event.getRawSlot() >= event.getInventory().getSize()) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!LogsCommand.isAuthorized(player)) {
            player.closeInventory();
            player.sendMessage(mm("<red>Access denied: You must be staff or OP to access audit logs.</red>"));
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);

        int slot = event.getRawSlot();
        switch (holder.getStage()) {
            case STAGE_1_CATEGORY -> handleStage1Click(player, slot);
            case STAGE_2_PLAYER -> handleStage2Click(player, holder, slot, clicked);
            case STAGE_3_TIME -> handleStage3Click(player, holder, slot);
            case STAGE_4_SNAPSHOT_HISTORY -> handleStage4Click(player, holder, slot, clicked);
        }
    }

    private void handleStage1Click(Player player, int slot) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        SubsystemCategory category = switch (slot) {
            case 11 -> SubsystemCategory.INVENTORY;
            case 13 -> SubsystemCategory.ENDER_CHEST;
            case 15 -> SubsystemCategory.POSITION;
            case 29 -> SubsystemCategory.ITEM_TRANSACTIONS;
            case 31 -> SubsystemCategory.BLOCK_ACTIONS;
            case 33 -> SubsystemCategory.SESSIONS;
            default -> null;
        };
        if (category != null) openStage2(player, category, 0);
    }

    private void handleStage2Click(Player player, LogsGuiHolder holder, int slot, ItemStack clicked) {
        if (slot == 45) { openStage1(player); return; }
        if (slot == 49) {
            player.closeInventory();
            activeChatPrompts.put(player.getUniqueId(), new ChatPrompt(player.getUniqueId(), PromptType.USERNAME, holder.getCategory(), null, System.currentTimeMillis()));
            player.sendMessage(mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <gray>Please type target player username in chat (or type <red>cancel</red>):</gray>"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
            return;
        }
        if (slot == 48 && holder.getPage() > 0) {
            openStage2(player, holder.getCategory(), holder.getPage() - 1);
            return;
        }
        if (slot == 50) {
            openStage2(player, holder.getCategory(), holder.getPage() + 1);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
            openStage3(player, holder.getCategory(), skullMeta.getOwningPlayer().getName());
        }
    }

    private void handleStage3Click(Player player, LogsGuiHolder holder, int slot) {
        String targetName = holder.getTargetPlayerName();
        SubsystemCategory category = holder.getCategory();

        switch (slot) {
            case 45 -> openStage2(player, category, 0);
            case 53 -> player.closeInventory();
            case 13 -> { player.closeInventory(); executeCategoryAction(player, category, targetName, Instant.now(), "Latest"); }
            case 20 -> { player.closeInventory(); executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(600), "10 Minutes Ago"); }
            case 21 -> { player.closeInventory(); executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(3600), "1 Hour Ago"); }
            case 22 -> { player.closeInventory(); executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(43200), "12 Hours Ago"); }
            case 23 -> { player.closeInventory(); executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(86400), "24 Hours Ago"); }
            case 24 -> { player.closeInventory(); new TimeInputGui(plugin, this, player, category, targetName, Instant.now()).open(player); }
            case 31 -> {
                if (category == SubsystemCategory.INVENTORY || category == SubsystemCategory.ENDER_CHEST) {
                    openStage4SnapshotHistory(player, category, targetName, 0);
                }
            }
            case 49 -> promptChatTime(player, category, targetName);
        }
    }

    private void handleStage4Click(Player player, LogsGuiHolder holder, int slot, ItemStack clicked) {
        if (slot == 45) { openStage3(player, holder.getCategory(), holder.getTargetPlayerName()); return; }
        if (slot == 48 && holder.getPage() > 0) {
            openStage4SnapshotHistory(player, holder.getCategory(), holder.getTargetPlayerName(), holder.getPage() - 1);
            return;
        }
        if (slot == 50) {
            openStage4SnapshotHistory(player, holder.getCategory(), holder.getTargetPlayerName(), holder.getPage() + 1);
            return;
        }

        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String displayName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (displayName.contains("snap-")) {
                int snapIndex = displayName.indexOf("snap-");
                String snapId = displayName.substring(snapIndex).trim().replace("]", "").replace(" ", "");
                player.closeInventory();
                if (holder.getCategory() == SubsystemCategory.ENDER_CHEST) {
                    logsCommand.queryAndOpenEchestById(player, holder.getTargetPlayerName(), snapId);
                } else {
                    logsCommand.queryAndOpenSnapshotById(player, holder.getTargetPlayerName(), snapId);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LogsGuiHolder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = activeChatPrompts.get(player.getUniqueId());
        if (prompt == null) return;

        if (System.currentTimeMillis() - prompt.createdMillis() > 90000L) {
            activeChatPrompts.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);

        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (text.equalsIgnoreCase("cancel")) {
            activeChatPrompts.remove(player.getUniqueId());
            player.sendMessage(mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <yellow>Cancelled audit chat prompt.</yellow>"));
            return;
        }

        if (prompt.type() == PromptType.USERNAME) {
            activeChatPrompts.remove(player.getUniqueId());
            String sanitized = text.replaceAll("[^a-zA-Z0-9_]", "");
            if (sanitized.isBlank()) {
                player.sendMessage(mm("<red>Invalid username format. Try again or type cancel.</red>"));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> openStage3(player, prompt.category(), sanitized));
            return;
        }

        if (prompt.type() == PromptType.TIMESTAMP) {
            Instant parsedInstant = logsCommand.parseRelativeOrAbsoluteTime(text);
            if (parsedInstant == null) {
                player.sendMessage(mm("<red>Invalid time or duration format <yellow>'" + text + "'</yellow>! Try <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow>, or <yellow>DD/MM/YYYY HH:MM:SS</yellow> (or type <yellow>cancel</yellow>).</red>"));
                return;
            }
            activeChatPrompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> executeCategoryAction(player, prompt.category(), prompt.targetPlayerName(), parsedInstant, text));
        }
    }

    public void promptChatTime(Player player, SubsystemCategory category, String targetPlayerName) {
        player.closeInventory();
        activeChatPrompts.put(player.getUniqueId(), new ChatPrompt(player.getUniqueId(), PromptType.TIMESTAMP, category, targetPlayerName, System.currentTimeMillis()));
        player.sendMessage(mm("<dark_gray>▪ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ▪</dark_gray>"));
        player.sendMessage(mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <dark_gray>▪</dark_gray> <gray>Time Input for</gray> <white>" + targetPlayerName + "</white> <gray>(Category: <aqua>" + category.getDisplayName() + "</aqua>)</gray>"));
        player.sendMessage(mm("<gray>Type your target time or duration directly in chat:</gray>"));
        player.sendMessage(mm("  <yellow>• Relative:</yellow> <white>5m, 10m, 30m, 1h, 6h, 12h, 1d, 7d, now, yesterday</white>"));
        player.sendMessage(mm("  <yellow>• Clock / Date:</yellow> <white>15:30:00, 15:30, 12/10/2026 18:30:00</white>"));
        Component clickToType = mm("<click:suggest_command:'/logs " + targetPlayerName + " '><hover:show_text:'<gray>Click to prefill /logs command in your chat bar</gray>'><aqua><bold>[⌨ CLICK TO ENTER TIME]</bold></aqua></click>  <click:run_command:'/logs cancelprompt'><hover:show_text:'<gray>Click to cancel prompt</gray>'><red><bold>[✕ CANCEL]</bold></red></click>");
        player.sendMessage(mm("<dark_gray>▪ </dark_gray>").append(clickToType));
        player.sendMessage(mm("<dark_gray>▪ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ▪</dark_gray>"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
    }

    public void cancelPrompt(Player player) {
        if (activeChatPrompts.remove(player.getUniqueId()) != null) {
            player.sendMessage(mm("<gradient:#c084fc:#f472b6><bold>▶ AUDIT LOGS</bold></gradient> <yellow>Cancelled audit chat prompt.</yellow>"));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeChatPrompts.remove(event.getPlayer().getUniqueId());
    }

    public void executeCategoryAction(Player player, SubsystemCategory category, String targetName, Instant instant, String displayTime) {
        switch (category) {
            case INVENTORY -> logsCommand.queryAndOpenSnapshot(player, targetName, instant, displayTime);
            case ENDER_CHEST -> logsCommand.queryAndOpenEchest(player, targetName, instant, displayTime);
            case POSITION -> logsCommand.handlePositionDirect(player, targetName, instant, displayTime);
            case ITEM_TRANSACTIONS -> logsCommand.handleItemsDirect(player, targetName, instant, displayTime);
            case BLOCK_ACTIONS -> logsCommand.handleActionsDirect(player, targetName, instant, displayTime);
            case SESSIONS -> logsCommand.handleSessionsDirect(player, targetName, instant, displayTime);
        }
    }

    private void applyDualToneBorder(Inventory inv) {
        ItemStack border = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.empty());
        border.setItemMeta(borderMeta);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 54; ++i) inv.setItem(i, filler);
        for (int i = 0; i < 9; ++i) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int slot : new int[]{9, 17, 18, 26, 27, 35, 36, 44}) {
            inv.setItem(slot, border);
        }
    }

    private ItemStack buildCategoryItem(SubsystemCategory category) {
        return buildUtilityItem(category.getIcon(), "<gradient:#e879f9:#c084fc><bold>" + category.getDisplayName().toUpperCase(Locale.ROOT) + "</bold></gradient>", category.getSubtitle(), category.getDescriptionLines(), "CLICK TO SELECT");
    }

    private ItemStack buildUtilityItem(Material material, String titleMiniMessage, String subtitle, List<String> bodyLines, String actionCallout) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm(titleMiniMessage));

        List<Component> lore = new ArrayList<>();
        lore.add(mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        if (subtitle != null && !subtitle.isBlank()) lore.add(mm("<yellow>" + subtitle + "</yellow>"));
        if (bodyLines != null) {
            for (String line : bodyLines) lore.add(mm("<gray>" + line + "</gray>"));
        }
        lore.add(mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"));
        lore.add(mm("<color:#ec4899><bold><underlined>" + actionCallout + "</underlined></bold></color> <gray>»</gray>"));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPlayerSkull(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(mm("<gradient:#e879f9:#c084fc><bold>" + target.getName() + "</bold></gradient>"));

        List<Component> lore = List.of(
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<yellow>Active Online Player</yellow>"),
                mm("<gray>Ping: <yellow>" + target.getPing() + " ms</yellow></gray>"),
                mm("<gray>Gamemode: <yellow>" + target.getGameMode().name() + "</yellow></gray>"),
                mm("<gray>World: <yellow>" + target.getWorld().getName() + "</yellow></gray>"),
                mm("<gray>Location: <yellow>" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ() + "</yellow></gray>"),
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<color:#ec4899><bold><underlined>CLICK TO SELECT</underlined></bold></color> <gray>»</gray>")
        );

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildSnapshotItem(SnapshotRow row) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm("<gradient:#e879f9:#c084fc><bold>" + row.id() + "</bold></gradient>"));

        List<Component> lore = List.of(
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<gray>Timestamp: <yellow>" + row.timestamp() + "</yellow></gray>"),
                mm("<gray>Reason: <white>" + row.reason() + "</white></gray>"),
                mm("<gray>World: <yellow>" + row.world() + "</yellow></gray>"),
                mm("<gray>Location: <yellow>" + (int) row.x() + ", " + (int) row.y() + ", " + (int) row.z() + "</yellow></gray>"),
                mm("<gray>Duration: <yellow>" + row.durationSeconds() + "s</yellow></gray>"),
                mm("<dark_gray>▪ --------------------------------- ▪</dark_gray>"),
                mm("<color:#ec4899><bold><underlined>CLICK TO INSPECT</underlined></bold></color> <gray>»</gray>")
        );

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    public static class LogsGuiHolder implements InventoryHolder {
        private final GuiStage stage;
        private final SubsystemCategory category;
        private final String targetPlayerName;
        private final int page;
        private Inventory inventory;

        public LogsGuiHolder(GuiStage stage, SubsystemCategory category, String targetPlayerName, int page) {
            this.stage = stage;
            this.category = category;
            this.targetPlayerName = targetPlayerName;
            this.page = page;
        }

        public GuiStage getStage() { return stage; }
        public SubsystemCategory getCategory() { return category; }
        public String getTargetPlayerName() { return targetPlayerName; }
        public int getPage() { return page; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }

        @Override
        public @NotNull Inventory getInventory() { return inventory; }
    }

    public enum GuiStage {
        STAGE_1_CATEGORY,
        STAGE_2_PLAYER,
        STAGE_3_TIME,
        STAGE_4_SNAPSHOT_HISTORY
    }

    public enum SubsystemCategory {
        INVENTORY("Inventory Snapshot", Material.GOLDEN_SWORD, Material.LIME_STAINED_GLASS_PANE, "Point-in-Time Inventory Log", List.of("Inspect complete player inventory, equipped armor,", "offhand items, and active potion effects at any moment.")),
        ENDER_CHEST("Ender Chest Vault", Material.ENDER_EYE, Material.PURPLE_STAINED_GLASS_PANE, "Ender Chest Historical Records", List.of("View item contents preserved inside the target's", "ender vault at specified historical points in time.")),
        POSITION("Position Trail", Material.RECOVERY_COMPASS, Material.CYAN_STAINED_GLASS_PANE, "Coordinate Breadcrumb Trail", List.of("Track historical world positions, pitch, yaw", "orientation, and timestamped movement breadcrumbs.")),
        ITEM_TRANSACTIONS("Item Transactions", Material.DIAMOND, Material.LIGHT_BLUE_STAINED_GLASS_PANE, "Item Pickup, Drop & Chest Logs", List.of("Monitor items picked up, dropped, deposited, or", "withdrawn by the player across gameplay sessions.")),
        BLOCK_ACTIONS("Block & Container Actions", Material.WRITTEN_BOOK, Material.ORANGE_STAINED_GLASS_PANE, "Action & Interaction History", List.of("Audit executed commands, container access, placed", "blocks, and broken blocks with world coordinates.")),
        SESSIONS("Session History & Client Platform", Material.CLOCK, Material.YELLOW_STAINED_GLASS_PANE, "Session & Platform Tracking", List.of("Inspect play sessions, login/logout timestamps,", "client platform (Java/Bedrock), and IP addresses."));

        private final String displayName;
        private final Material icon;
        private final Material pedestal;
        private final String subtitle;
        private final List<String> descriptionLines;

        SubsystemCategory(String displayName, Material icon, Material pedestal, String subtitle, List<String> descriptionLines) {
            this.displayName = displayName;
            this.icon = icon;
            this.pedestal = pedestal;
            this.subtitle = subtitle;
            this.descriptionLines = descriptionLines;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public Material getPedestal() { return pedestal; }
        public String getSubtitle() { return subtitle; }
        public List<String> getDescriptionLines() { return descriptionLines; }
    }

    public record ChatPrompt(UUID staffUuid, PromptType type, SubsystemCategory category, String targetPlayerName, long createdMillis) {}

    public enum PromptType {
        USERNAME,
        TIMESTAMP
    }

    public record SnapshotRow(String id, String timestamp, String reason, String world, double x, double y, double z, int durationSeconds) {}
}
