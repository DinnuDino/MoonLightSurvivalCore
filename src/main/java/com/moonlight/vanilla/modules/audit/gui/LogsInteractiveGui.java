/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.audit.gui;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.commands.LogsCommand;
import com.moonlight.vanilla.modules.audit.gui.TimeInputGui;
import com.moonlight.vanilla.utils.VanishUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class LogsInteractiveGui
implements Listener {
    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsCommand logsCommand;
    private final Map<UUID, ChatPrompt> activeChatPrompts = new ConcurrentHashMap<UUID, ChatPrompt>();

    public LogsInteractiveGui(MoonlightVanillaSurvivalCore plugin, LogsCommand logsCommand) {
        this.plugin = plugin;
        this.logsCommand = logsCommand;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
    }

    public LogsCommand getLogsCommand() {
        return this.logsCommand;
    }

    public void openStage1(Player player) {
        Component title = MiniMessage.miniMessage().deserialize("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <dark_gray>\u25aa</dark_gray> <gray>Select Category</gray>");
        LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_1_CATEGORY, null, null, 0);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (Component)title);
        holder.setInventory(inv);
        this.applyDualToneBorder(inv);
        inv.setItem(11, this.buildCategoryItem(SubsystemCategory.INVENTORY));
        inv.setItem(13, this.buildCategoryItem(SubsystemCategory.ENDER_CHEST));
        inv.setItem(15, this.buildCategoryItem(SubsystemCategory.POSITION));
        inv.setItem(29, this.buildCategoryItem(SubsystemCategory.ITEM_TRANSACTIONS));
        inv.setItem(31, this.buildCategoryItem(SubsystemCategory.BLOCK_ACTIONS));
        inv.setItem(33, this.buildCategoryItem(SubsystemCategory.SESSIONS));
        inv.setItem(49, this.buildUtilityItem(Material.BARRIER, "<red><bold>CLOSE MENU</bold></red>", "Exit Audit System", List.of("Close the interactive audit menu."), "CLICK TO CLOSE"));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.1f);
    }

    public void openStage2(Player player, SubsystemCategory category, int page) {
        Component title = MiniMessage.miniMessage().deserialize("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <dark_gray>\u25aa</dark_gray> <gray>Select Target Player</gray>");
        LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_2_PLAYER, category, null, page);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (Component)title);
        holder.setInventory(inv);
        this.applyDualToneBorder(inv);
        ArrayList<Player> onlinePlayers = new ArrayList<Player>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!VanishUtil.canSee((CommandSender)player, p)) continue;
            onlinePlayers.add(p);
        }
        onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int[] usableSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int pageSize = usableSlots.length;
        int totalPages = Math.max(1, (int)Math.ceil((double)onlinePlayers.size() / (double)pageSize));
        int safePage = Math.min(Math.max(0, page), totalPages - 1);
        int startIndex = safePage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, onlinePlayers.size());
        for (int i = startIndex; i < endIndex; ++i) {
            Player target = (Player)onlinePlayers.get(i);
            int slot = usableSlots[i - startIndex];
            inv.setItem(slot, this.buildPlayerSkull(target));
        }
        inv.setItem(45, this.buildUtilityItem(Material.ARROW, "<gold><bold>BACK TO CATEGORIES</bold></gold>", "Category Selection", List.of("Return to Stage 1 category menu."), "CLICK TO RETURN"));
        inv.setItem(49, this.buildUtilityItem(Material.NAME_TAG, "<gradient:#e879f9:#c084fc><bold>TYPE CUSTOM / OFFLINE USERNAME</bold></gradient>", "Custom Player Target", List.of("Close inventory and type any offline", "or historical username via server chat."), "CLICK TO ENTER CHAT INPUT"));
        if (safePage > 0) {
            inv.setItem(48, this.buildUtilityItem(Material.ARROW, "<aqua><bold>PREVIOUS PAGE</bold></aqua>", "Page " + safePage + " of " + totalPages, List.of("View previous players."), "CLICK TO VIEW"));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(50, this.buildUtilityItem(Material.ARROW, "<aqua><bold>NEXT PAGE</bold></aqua>", "Page " + (safePage + 2) + " of " + totalPages, List.of("View next players."), "CLICK TO VIEW"));
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
    }

    public void openStage3(Player player, SubsystemCategory category, String targetPlayerName) {
        Component title = MiniMessage.miniMessage().deserialize(("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <dark_gray>\u25aa</dark_gray> <gray>" + targetPlayerName + "</gray>"));
        LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_3_TIME, category, targetPlayerName, 0);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (Component)title);
        holder.setInventory(inv);
        this.applyDualToneBorder(inv);
        Player onlineTarget = Bukkit.getPlayerExact((String)targetPlayerName);
        boolean isOnline = onlineTarget != null && onlineTarget.isOnline();
        boolean isLiveEligible = (category == SubsystemCategory.INVENTORY || category == SubsystemCategory.ENDER_CHEST) && isOnline;
        inv.setItem(13, this.buildUtilityItem(Material.NETHER_STAR, isLiveEligible ? "<gradient:#a7f3d0:#34d399><bold>LIVE ACTIVE INSPECTION</bold></gradient>" : "<gradient:#e879f9:#c084fc><bold>LATEST LOG RECORD</bold></gradient>", isLiveEligible ? "Real-time 10-Tick Sync" : "Most Recent Database Record", List.of(isLiveEligible ? "Opens real-time active inspection with" : "Queries the most recent record", isLiveEligible ? "live synchronization from " + targetPlayerName + "." : "saved in database for " + targetPlayerName + "."), "CLICK TO SELECT"));
        inv.setItem(20, this.buildUtilityItem(Material.CLOCK, "<gradient:#e879f9:#c084fc><bold>10 MINUTES AGO</bold></gradient>", "Quick Rewind (-10m)", List.of("Inspect audit state 10 minutes", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(21, this.buildUtilityItem(Material.COMPASS, "<gradient:#e879f9:#c084fc><bold>1 HOUR AGO</bold></gradient>", "Quick Rewind (-1h)", List.of("Inspect audit state 1 hour", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(22, this.buildUtilityItem(Material.SPYGLASS, "<gradient:#e879f9:#c084fc><bold>12 HOURS AGO</bold></gradient>", "Quick Rewind (-12h)", List.of("Inspect audit state 12 hours", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(23, this.buildUtilityItem(Material.BOOK, "<gradient:#e879f9:#c084fc><bold>24 HOURS AGO (1 DAY)</bold></gradient>", "Quick Rewind (-24h)", List.of("Inspect audit state 24 hours", "prior to the current time."), "CLICK TO SELECT"));
        inv.setItem(24, this.buildUtilityItem(Material.ANVIL, "<gradient:#38bdf8:#0284c7><bold>\u2328 ENTER TIME DIRECTLY (GUI)</bold></gradient>", "Direct Anvil Input & Presets", List.of("Type exact time or duration (e.g. 10m, 1h,", "12h, 1d, 15:30:00) directly on screen."), "CLICK TO OPEN TIME SELECTOR"));
        if (category == SubsystemCategory.INVENTORY || category == SubsystemCategory.ENDER_CHEST) {
            inv.setItem(31, this.buildUtilityItem(Material.CHEST_MINECART, "<gradient:#e879f9:#c084fc><bold>BROWSE SNAPSHOT HISTORY</bold></gradient>", "Database Snapshot Archive", List.of("Opens a paginated list of actual database", "snapshot rows recorded for " + targetPlayerName + "."), "CLICK TO BROWSE"));
        }
        inv.setItem(45, this.buildUtilityItem(Material.ARROW, "<gold><bold>BACK TO PLAYERS</bold></gold>", "Player Selection", List.of("Return to target player selector."), "CLICK TO RETURN"));
        inv.setItem(49, this.buildUtilityItem(Material.WRITABLE_BOOK, "<gradient:#ec4899:#db2777><bold>\ud83d\udcac TYPE IN CHAT (AUTHME STYLE)</bold></gradient>", "Direct Chat Prompt & Auto-Fill", List.of("Prompts you in chat just like AuthMe login!", "Type 10m, 1h, 1d, 15:30:00, or click", "interactive chat button to auto-fill."), "CLICK TO PROMPT IN CHAT"));
        inv.setItem(53, this.buildUtilityItem(Material.BARRIER, "<red><bold>CLOSE MENU</bold></red>", "Exit", List.of("Close audit inspection menu."), "CLICK TO CLOSE"));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.3f);
    }

    public void openStage4SnapshotHistory(Player player, SubsystemCategory category, String targetPlayerName, int page) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Querying snapshot database archives for <white>" + targetPlayerName + "</white>...</gray>")));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            ArrayList<SnapshotRow> snapshots = new ArrayList<SnapshotRow>();
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = "SELECT snapshot_id, timestamp, snapshot_reason, world_name, x, y, z, duration_seconds FROM player_inventory_snapshots WHERE username = ? ORDER BY id DESC LIMIT 28 OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, targetPlayerName);
                    ps.setInt(2, page * 28);
                    try (ResultSet rs = ps.executeQuery();){
                        while (rs.next()) {
                            snapshots.add(new SnapshotRow(rs.getString("snapshot_id"), rs.getString("timestamp"), rs.getString("snapshot_reason"), rs.getString("world_name"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getInt("duration_seconds")));
                        }
                    }
                }
            }
            catch (Exception e) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Database error loading snapshots: " + e.getMessage() + "</red>")));
                return;
            }
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                Component title = MiniMessage.miniMessage().deserialize(("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <dark_gray>\u25aa</dark_gray> <gray>" + targetPlayerName + " Archive</gray>"));
                LogsGuiHolder holder = new LogsGuiHolder(GuiStage.STAGE_4_SNAPSHOT_HISTORY, category, targetPlayerName, page);
                Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)54, (Component)title);
                holder.setInventory(inv);
                this.applyDualToneBorder(inv);
                int[] usableSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
                for (int i = 0; i < snapshots.size(); ++i) {
                    SnapshotRow row = (SnapshotRow)snapshots.get(i);
                    inv.setItem(usableSlots[i], this.buildSnapshotItem(row));
                }
                inv.setItem(45, this.buildUtilityItem(Material.ARROW, "<gold><bold>BACK TO TIME OPTIONS</bold></gold>", "Time Selection", List.of("Return to snapshot time options."), "CLICK TO RETURN"));
                if (page > 0) {
                    inv.setItem(48, this.buildUtilityItem(Material.ARROW, "<aqua><bold>PREVIOUS PAGE</bold></aqua>", "Page " + page, List.of("View previous snapshots."), "CLICK TO VIEW"));
                }
                if (snapshots.size() == 28) {
                    inv.setItem(50, this.buildUtilityItem(Material.ARROW, "<aqua><bold>NEXT PAGE</bold></aqua>", "Page " + (page + 2), List.of("View older snapshots."), "CLICK TO VIEW"));
                }
                player.openInventory(inv);
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
            });
        });
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof LogsGuiHolder)) {
            return;
        }
        LogsGuiHolder holder = (LogsGuiHolder)inventoryHolder;
        if (event.getRawSlot() >= event.getInventory().getSize()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!LogsCommand.isAuthorized((CommandSender)player)) {
            player.closeInventory();
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Access denied: You must be staff or OP to access audit logs.</red>"));
            return;
        }
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        switch (holder.getStage().ordinal()) {
            case 0: {
                this.handleStage1Click(player, slot);
                break;
            }
            case 1: {
                this.handleStage2Click(player, holder, slot, clicked);
                break;
            }
            case 2: {
                this.handleStage3Click(player, holder, slot);
                break;
            }
            case 3: {
                this.handleStage4Click(player, holder, slot, clicked);
            }
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
        if (category != null) {
            this.openStage2(player, category, 0);
        }
    }

    private void handleStage2Click(Player player, LogsGuiHolder holder, int slot, ItemStack clicked) {
        SkullMeta skullMeta;
        ItemMeta itemMeta;
        if (slot == 45) {
            this.openStage1(player);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            this.activeChatPrompts.put(player.getUniqueId(), new ChatPrompt(player.getUniqueId(), PromptType.USERNAME, holder.getCategory(), null, System.currentTimeMillis()));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <gray>Please type target player username in chat (or type <red>cancel</red>):</gray>"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
            return;
        }
        if (slot == 48 && holder.getPage() > 0) {
            this.openStage2(player, holder.getCategory(), holder.getPage() - 1);
            return;
        }
        if (slot == 50) {
            this.openStage2(player, holder.getCategory(), holder.getPage() + 1);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && (itemMeta = clicked.getItemMeta()) instanceof SkullMeta && (skullMeta = (SkullMeta)itemMeta).getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
            String targetName = skullMeta.getOwningPlayer().getName();
            this.openStage3(player, holder.getCategory(), targetName);
        }
    }

    private void handleStage3Click(Player player, LogsGuiHolder holder, int slot) {
        String targetName = holder.getTargetPlayerName();
        SubsystemCategory category = holder.getCategory();
        if (slot == 45) {
            this.openStage2(player, category, 0);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        if (slot == 13) {
            player.closeInventory();
            this.executeCategoryAction(player, category, targetName, Instant.now(), "Latest");
            return;
        }
        if (slot == 20) {
            player.closeInventory();
            this.executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(600L), "10 Minutes Ago");
            return;
        }
        if (slot == 21) {
            player.closeInventory();
            this.executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(3600L), "1 Hour Ago");
            return;
        }
        if (slot == 22) {
            player.closeInventory();
            this.executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(43200L), "12 Hours Ago");
            return;
        }
        if (slot == 23) {
            player.closeInventory();
            this.executeCategoryAction(player, category, targetName, Instant.now().minusSeconds(86400L), "24 Hours Ago");
            return;
        }
        if (slot == 24) {
            player.closeInventory();
            new TimeInputGui(this.plugin, this, player, category, targetName, Instant.now()).open(player);
            return;
        }
        if (slot == 31 && (category == SubsystemCategory.INVENTORY || category == SubsystemCategory.ENDER_CHEST)) {
            this.openStage4SnapshotHistory(player, category, targetName, 0);
            return;
        }
        if (slot == 49) {
            this.promptChatTime(player, category, targetName);
            return;
        }
    }

    private void handleStage4Click(Player player, LogsGuiHolder holder, int slot, ItemStack clicked) {
        String displayName;
        if (slot == 45) {
            this.openStage3(player, holder.getCategory(), holder.getTargetPlayerName());
            return;
        }
        if (slot == 48 && holder.getPage() > 0) {
            this.openStage4SnapshotHistory(player, holder.getCategory(), holder.getTargetPlayerName(), holder.getPage() - 1);
            return;
        }
        if (slot == 50) {
            this.openStage4SnapshotHistory(player, holder.getCategory(), holder.getTargetPlayerName(), holder.getPage() + 1);
            return;
        }
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() && (displayName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())).contains("snap-")) {
            int snapIndex = displayName.indexOf("snap-");
            String snapId = displayName.substring(snapIndex).trim().replace("]", "").replace(" ", "");
            player.closeInventory();
            if (holder.getCategory() == SubsystemCategory.ENDER_CHEST) {
                this.logsCommand.queryAndOpenEchestById(player, holder.getTargetPlayerName(), snapId);
            } else {
                this.logsCommand.queryAndOpenSnapshotById(player, holder.getTargetPlayerName(), snapId);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LogsGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = this.activeChatPrompts.get(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        if (System.currentTimeMillis() - prompt.createdMillis() > 90000L) {
            this.activeChatPrompts.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (text.equalsIgnoreCase("cancel")) {
            this.activeChatPrompts.remove(player.getUniqueId());
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <yellow>Cancelled audit chat prompt.</yellow>"));
            return;
        }
        if (prompt.type() == PromptType.USERNAME) {
            this.activeChatPrompts.remove(player.getUniqueId());
            String sanitizedUsername = text.replaceAll("[^a-zA-Z0-9_]", "");
            if (sanitizedUsername.isBlank()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid username format. Try again or type cancel.</red>"));
                return;
            }
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openStage3(player, prompt.category(), sanitizedUsername));
            return;
        }
        if (prompt.type() == PromptType.TIMESTAMP) {
            Instant parsedInstant = this.logsCommand.parseRelativeOrAbsoluteTime(text);
            if (parsedInstant == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Invalid time or duration format <yellow>'" + text + "'</yellow>! Try <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow>, or <yellow>DD/MM/YYYY HH:MM:SS</yellow> (or type <yellow>cancel</yellow>).</red>")));
                return;
            }
            this.activeChatPrompts.remove(player.getUniqueId());
            Instant finalInstant = parsedInstant;
            String displayText = text;
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.executeCategoryAction(player, prompt.category(), prompt.targetPlayerName(), finalInstant, displayText));
        }
    }

    public void promptChatTime(Player player, SubsystemCategory category, String targetPlayerName) {
        player.closeInventory();
        this.activeChatPrompts.put(player.getUniqueId(), new ChatPrompt(player.getUniqueId(), PromptType.TIMESTAMP, category, targetPlayerName, System.currentTimeMillis()));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u25aa</dark_gray>"));
        player.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <dark_gray>\u25aa</dark_gray> <gray>Time Input for</gray> <white>" + targetPlayerName + "</white> <gray>(Category: <aqua>" + category.getDisplayName() + "</aqua>)</gray>")));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Type your target time or duration directly in chat:</gray>"));
        player.sendMessage(MiniMessage.miniMessage().deserialize("  <yellow>\u2022 Relative:</yellow> <white>5m, 10m, 30m, 1h, 6h, 12h, 1d, 7d, now, yesterday</white>"));
        player.sendMessage(MiniMessage.miniMessage().deserialize("  <yellow>\u2022 Clock / Date:</yellow> <white>15:30:00, 15:30, 12/10/2026 18:30:00</white>"));
        Component clickToType = MiniMessage.miniMessage().deserialize(("<click:suggest_command:'/logs " + targetPlayerName + " '><hover:show_text:'<gray>Click to prefill /logs command in your chat bar</gray>'><aqua><bold>[\u2328 CLICK TO ENTER TIME]</bold></aqua></click>  <click:run_command:'/logs cancelprompt'><hover:show_text:'<gray>Click to cancel prompt</gray>'><red><bold>[\u2715 CANCEL]</bold></red></click>"));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa </dark_gray>").append(clickToType));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 \u25aa</dark_gray>"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
    }

    public void cancelPrompt(Player player) {
        if (this.activeChatPrompts.remove(player.getUniqueId()) != null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#c084fc:#f472b6><bold>\u25b6 AUDIT LOGS</bold></gradient> <yellow>Cancelled audit chat prompt.</yellow>"));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.activeChatPrompts.remove(event.getPlayer().getUniqueId());
    }

    public void executeCategoryAction(Player player, SubsystemCategory category, String targetName, Instant instant, String displayTime) {
        switch (category.ordinal()) {
            case 0: {
                this.logsCommand.queryAndOpenSnapshot((CommandSender)player, targetName, instant, displayTime);
                break;
            }
            case 1: {
                this.logsCommand.queryAndOpenEchest((CommandSender)player, targetName, instant, displayTime);
                break;
            }
            case 2: {
                this.logsCommand.handlePositionDirect((CommandSender)player, targetName, instant, displayTime);
                break;
            }
            case 3: {
                this.logsCommand.handleItemsDirect((CommandSender)player, targetName, instant, displayTime);
                break;
            }
            case 4: {
                this.logsCommand.handleActionsDirect((CommandSender)player, targetName, instant, displayTime);
                break;
            }
            case 5: {
                this.logsCommand.handleSessionsDirect((CommandSender)player, targetName, instant, displayTime);
            }
        }
    }

    private void applyDualToneBorder(Inventory inv) {
        int[] lateralSlots;
        int i;
        ItemStack border = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName((Component)Component.empty());
        border.setItemMeta(borderMeta);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName((Component)Component.empty());
        filler.setItemMeta(fillerMeta);
        for (i = 0; i < 54; ++i) {
            inv.setItem(i, filler);
        }
        for (i = 0; i < 9; ++i) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int slot : lateralSlots = new int[]{9, 17, 18, 26, 27, 35, 36, 44}) {
            inv.setItem(slot, border);
        }
    }

    private ItemStack buildCategoryItem(SubsystemCategory category) {
        return this.buildUtilityItem(category.getIcon(), "<gradient:#e879f9:#c084fc><bold>" + category.getDisplayName().toUpperCase() + "</bold></gradient>", category.getSubtitle(), category.getDescriptionLines(), "CLICK TO SELECT");
    }

    private ItemStack buildPedestalItem(Material material, String labelMiniMessage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(labelMiniMessage));
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildUtilityItem(Material material, String titleMiniMessage, String subtitle, List<String> bodyLines, String actionCallout) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(titleMiniMessage));
        ArrayList<Component> lore = new ArrayList<Component>();
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa --------------------------------- \u25aa</dark_gray>"));
        if (subtitle != null && !subtitle.isBlank()) {
            lore.add(MiniMessage.miniMessage().deserialize(("<yellow>" + subtitle + "</yellow>")));
        }
        if (bodyLines != null) {
            for (String line : bodyLines) {
                lore.add(MiniMessage.miniMessage().deserialize(("<gray>" + line + "</gray>")));
            }
        }
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa --------------------------------- \u25aa</dark_gray>"));
        lore.add(MiniMessage.miniMessage().deserialize(("<color:#ec4899><bold><underlined>" + actionCallout + "</underlined></bold></color> <gray>\u00bb</gray>")));
        meta.lore(lore);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPlayerSkull(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta)skull.getItemMeta();
        meta.setOwningPlayer((OfflinePlayer)target);
        meta.displayName(MiniMessage.miniMessage().deserialize(("<gradient:#e879f9:#c084fc><bold>" + target.getName() + "</bold></gradient>")));
        ArrayList<Component> lore = new ArrayList<Component>();
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa --------------------------------- \u25aa</dark_gray>"));
        lore.add(MiniMessage.miniMessage().deserialize("<yellow>Active Online Player</yellow>"));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Ping: <yellow>" + target.getPing() + " ms</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Gamemode: <yellow>" + target.getGameMode().name() + "</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>World: <yellow>" + target.getWorld().getName() + "</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Location: <yellow>" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ() + "</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa --------------------------------- \u25aa</dark_gray>"));
        lore.add(MiniMessage.miniMessage().deserialize("<color:#ec4899><bold><underlined>CLICK TO SELECT</underlined></bold></color> <gray>\u00bb</gray>"));
        meta.lore(lore);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
        skull.setItemMeta((ItemMeta)meta);
        return skull;
    }

    private ItemStack buildSnapshotItem(SnapshotRow row) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(("<gradient:#e879f9:#c084fc><bold>" + row.id() + "</bold></gradient>")));
        ArrayList<Component> lore = new ArrayList<Component>();
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa --------------------------------- \u25aa</dark_gray>"));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Timestamp: <yellow>" + row.timestamp() + "</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Reason: <white>" + row.reason() + "</white></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>World: <yellow>" + row.world() + "</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Location: <yellow>" + (int)row.x() + ", " + (int)row.y() + ", " + (int)row.z() + "</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(("<gray>Duration: <yellow>" + row.durationSeconds() + "s</yellow></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>\u25aa --------------------------------- \u25aa</dark_gray>"));
        lore.add(MiniMessage.miniMessage().deserialize("<color:#ec4899><bold><underlined>CLICK TO INSPECT</underlined></bold></color> <gray>\u00bb</gray>"));
        meta.lore(lore);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
        item.setItemMeta(meta);
        return item;
    }

    public static class LogsGuiHolder
    implements InventoryHolder {
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

        public GuiStage getStage() {
            return this.stage;
        }

        public SubsystemCategory getCategory() {
            return this.category;
        }

        public String getTargetPlayerName() {
            return this.targetPlayerName;
        }

        public int getPage() {
            return this.page;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @NotNull
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    public static enum GuiStage {
        STAGE_1_CATEGORY,
        STAGE_2_PLAYER,
        STAGE_3_TIME,
        STAGE_4_SNAPSHOT_HISTORY;

    }

    public static enum SubsystemCategory {
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

        private SubsystemCategory(String displayName, Material icon, Material pedestal, String subtitle, List<String> descriptionLines) {
            this.displayName = displayName;
            this.icon = icon;
            this.pedestal = pedestal;
            this.subtitle = subtitle;
            this.descriptionLines = descriptionLines;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public Material getIcon() {
            return this.icon;
        }

        public Material getPedestal() {
            return this.pedestal;
        }

        public String getSubtitle() {
            return this.subtitle;
        }

        public List<String> getDescriptionLines() {
            return this.descriptionLines;
        }
    }

    private record ChatPrompt(UUID staffUuid, PromptType type, SubsystemCategory category, String targetPlayerName, long createdMillis) {
    }

    public static enum PromptType {
        USERNAME,
        TIMESTAMP;

    }

    private record SnapshotRow(String id, String timestamp, String reason, String world, double x, double y, double z, int durationSeconds) {
    }
}

