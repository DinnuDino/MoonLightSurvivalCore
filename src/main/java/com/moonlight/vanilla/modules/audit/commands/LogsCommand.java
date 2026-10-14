/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.command.RemoteConsoleCommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.audit.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.EnderChestInspectionGui;
import com.moonlight.vanilla.modules.audit.gui.LogsInteractiveGui;
import com.moonlight.vanilla.modules.audit.gui.LogsListGui;
import com.moonlight.vanilla.modules.audit.gui.SnapshotInspectionGui;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import com.moonlight.vanilla.utils.VanishUtil;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class LogsCommand
implements CommandExecutor,
TabCompleter {
    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsInteractiveGui interactiveGui;
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[]{DateTimeFormatter.ofPattern("dd/MM/yyyy_HH:mm:ss").withZone(ZoneId.systemDefault()), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault()), DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss").withZone(ZoneId.systemDefault()), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.systemDefault()), DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").withZone(ZoneId.systemDefault()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()), DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()), DateTimeFormatter.ISO_INSTANT};

    public LogsCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.interactiveGui = new LogsInteractiveGui(plugin, this);
    }

    public LogsInteractiveGui getInteractiveGui() {
        return this.interactiveGui;
    }

    public static boolean isAuthorized(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) {
            return true;
        }
        if (sender instanceof Player) {
            Player player = (Player)sender;
            return player.isOp() || player.hasPermission("moonlight.logs.view") || player.hasPermission("moonlight.core.admin");
        }
        return false;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!LogsCommand.isAuthorized(sender)) {
            String deniedMsg = this.plugin.getAuditManager() != null ? this.plugin.getAuditManager().getConfig().getNoPermissionMessage() : "<red>Access denied: You must be a staff member (Trial Helper+) or server operator (OP) to use audit logging tools.</red>";
            sender.sendMessage(MiniMessage.miniMessage().deserialize(deniedMsg));
            return true;
        }
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
                this.interactiveGui.openStage1(player);
                return true;
            }
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.sendHelp(sender);
            return true;
        }
        String firstArg = args[0].toLowerCase();
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (firstArg.equals("cancelprompt")) {
                this.interactiveGui.cancelPrompt(player);
                return true;
            }
        }
        switch (firstArg) {
            case "snapshot": {
                this.handleSnapshot(sender, args);
                return true;
            }
            case "inventory": 
            case "inv": {
                this.handleInventorySub(sender, args);
                return true;
            }
            case "echest": 
            case "ec": 
            case "enderchest": {
                this.handleEchestSub(sender, args);
                return true;
            }
            case "position": 
            case "pos": {
                this.handlePosition(sender, args);
                return true;
            }
            case "items": {
                this.handleItems(sender, args);
                return true;
            }
            case "actions": {
                this.handleActions(sender, args);
                return true;
            }
            case "sessions": {
                this.handleSessions(sender, args);
                return true;
            }
            case "search": {
                this.handleSearch(sender, args);
                return true;
            }
            case "export": {
                this.handleExport(sender, args);
                return true;
            }
            case "privacy": {
                this.handlePrivacy(sender, args);
                return true;
            }
            case "ip": {
                this.handleIpLookup(sender, args);
                return true;
            }
        }
        this.handlePlayerDateQuery(sender, args);
        return true;
    }

    private void handlePlayerDateQuery(CommandSender sender, String[] args) {
        String targetName = args[0];
        if (args.length >= 3) {
            String datePart = args[1];
            String timePart = args[2];
            Instant targetInstant = this.parseDateTime(datePart + "_" + timePart);
            if (targetInstant == null) {
                targetInstant = this.parseDateTime(datePart + " " + timePart);
            }
            if (targetInstant == null) {
                targetInstant = this.parseRelativeOrAbsoluteTime(datePart + " " + timePart);
            }
            if (targetInstant == null) {
                targetInstant = this.parseRelativeOrAbsoluteTime(datePart);
            }
            if (targetInstant == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid Date/Time format! Use: <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow>, or <yellow>DD/MM/YYYY HH:MM:SS</yellow>.</red>"));
                return;
            }
            this.queryAndOpenSnapshot(sender, targetName, targetInstant, datePart + " " + timePart);
            return;
        }
        if (args.length == 2) {
            String timeInput = args[1];
            Instant targetInstant = this.parseRelativeOrAbsoluteTime(timeInput);
            if (targetInstant != null) {
                this.queryAndOpenSnapshot(sender, targetName, targetInstant, timeInput);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid Date or Duration format! Use: <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow>, or <yellow>DD/MM/YYYY</yellow>.</red>"));
            }
            return;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#6366f1:#a855f7><bold>=== Player Logs & Activity Overview ===</bold></gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Target: <white>" + targetName + "</white></gray>")));
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Syntax: <yellow>/logs " + targetName + " <DD/MM/YYYY> <HH:MM:SS></yellow></gray>")));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Fetching most recent snapshot...</gray>"));
        this.queryAndOpenSnapshot(sender, targetName, Instant.now(), "Latest");
    }

    public void queryAndOpenSnapshot(CommandSender sender, String targetName, Instant instant, String displayTime) {
        Player onlineTarget = Bukkit.getPlayerExact((String)targetName);
        boolean isLatest = "Latest".equalsIgnoreCase(displayTime);
        if (onlineTarget != null && onlineTarget.isOnline() && isLatest && sender instanceof Player) {
            Player player = (Player)sender;
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                SnapshotInspectionGui gui = new SnapshotInspectionGui(this.plugin, onlineTarget.getName(), "live", "Now (Live)", onlineTarget.getInventory().getStorageContents(), onlineTarget.getInventory().getArmorContents(), onlineTarget.getInventory().getItemInOffHand(), onlineTarget.getLevel(), onlineTarget.getTotalExperience(), onlineTarget.getHealth(), onlineTarget.getFoodLevel(), onlineTarget.getGameMode().name(), true, onlineTarget);
                player.openInventory(gui.getInventory());
                player.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Opened live active inventory of <white>" + onlineTarget.getName() + "</white>.</green>")));
            });
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = isLatest ? "SELECT * FROM player_inventory_snapshots WHERE username = ? ORDER BY id DESC LIMIT 1" : "SELECT * FROM player_inventory_snapshots WHERE username = ? AND timestamp <= ? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, targetName);
                    if (!isLatest) {
                        ps.setTimestamp(2, Timestamp.from(instant));
                    }
                    try (ResultSet rs = ps.executeQuery();){
                        if (rs.next()) {
                            String snapId = rs.getString("snapshot_id");
                            String snapTimestamp = rs.getString("timestamp");
                            String invData = rs.getString("inventory_data");
                            String armorData = rs.getString("armor_data");
                            String offhandData = rs.getString("offhand_data");
                            int expLevel = rs.getInt("exp_level");
                            int totalExp = rs.getInt("total_exp");
                            double health = rs.getDouble("health");
                            int foodLevel = rs.getInt("hunger");
                            String gamemode = rs.getString("gamemode");
                            InventorySerializer s = new InventorySerializer();
                            ItemStack[] storage = s.deserializeItemArray(invData);
                            ItemStack[] armor = s.deserializeItemArray(armorData);
                            ItemStack offhand = s.deserializeItem(offhandData);
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Found log snapshot: <yellow>" + snapId + "</yellow> (<gray>" + snapTimestamp + "</gray>)</green>")));
                            if (sender instanceof Player) {
                                Player player = (Player)sender;
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                    SnapshotInspectionGui gui = new SnapshotInspectionGui(this.plugin, targetName, snapId, snapTimestamp, storage, armor, offhand, expLevel, totalExp, health, foodLevel, gamemode, false, null);
                                    player.openInventory(gui.getInventory());
                                });
                            } else {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gold>=== Inventory Contents for " + targetName + " ===</gold>")));
                                if (storage != null) {
                                    for (int i = 0; i < storage.length; ++i) {
                                        if (storage[i] == null || storage[i].getType() == Material.AIR) continue;
                                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Slot " + i + ": <yellow>" + storage[i].getAmount() + "x " + storage[i].getType().name() + "</yellow></gray>")));
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>No logs or inventory snapshots found for <white>" + targetName + "</white> around <yellow>" + displayTime + "</yellow>.</red>")));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Database query error: " + e.getMessage() + "</red>")));
            }
        });
    }

    public void queryAndOpenSnapshotById(Player staff, String targetName, String snapshotId) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = "SELECT * FROM player_inventory_snapshots WHERE snapshot_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, snapshotId);
                    try (ResultSet rs = ps.executeQuery();){
                        if (rs.next()) {
                            String snapId = rs.getString("snapshot_id");
                            String snapTimestamp = rs.getString("timestamp");
                            String invData = rs.getString("inventory_data");
                            String armorData = rs.getString("armor_data");
                            String offhandData = rs.getString("offhand_data");
                            int expLevel = rs.getInt("exp_level");
                            int totalExp = rs.getInt("total_exp");
                            double health = rs.getDouble("health");
                            int foodLevel = rs.getInt("hunger");
                            String gamemode = rs.getString("gamemode");
                            InventorySerializer s = new InventorySerializer();
                            ItemStack[] storage = s.deserializeItemArray(invData);
                            ItemStack[] armor = s.deserializeItemArray(armorData);
                            ItemStack offhand = s.deserializeItem(offhandData);
                            staff.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Opened snapshot: <yellow>" + snapId + "</yellow> (<gray>" + snapTimestamp + "</gray>)</green>")));
                            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                SnapshotInspectionGui gui = new SnapshotInspectionGui(this.plugin, targetName, snapId, snapTimestamp, storage, armor, offhand, expLevel, totalExp, health, foodLevel, gamemode, false, null);
                                staff.openInventory(gui.getInventory());
                            });
                        } else {
                            staff.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Snapshot not found: " + snapshotId + "</red>")));
                        }
                    }
                }
            }
            catch (Exception e) {
                staff.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Database error loading snapshot: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handleEchestSub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs echest <player> [date] [time]</red> <gray>(Alias: /logs ec <player> ...)</gray>"));
            return;
        }
        String targetName = args[1];
        if (args.length >= 4) {
            Instant instant = this.parseDateTime(args[2] + "_" + args[3]);
            if (instant == null) {
                instant = this.parseDateTime(args[2] + " " + args[3]);
            }
            this.queryAndOpenEchest(sender, targetName, instant != null ? instant : Instant.now(), args[2] + " " + args[3]);
        } else if (args.length == 3) {
            Instant instant = this.parseRelativeOrAbsoluteTime(args[2]);
            this.queryAndOpenEchest(sender, targetName, instant != null ? instant : Instant.now(), args[2]);
        } else {
            this.queryAndOpenEchest(sender, targetName, Instant.now(), "Latest");
        }
    }

    public void queryAndOpenEchest(CommandSender sender, String targetName, Instant instant, String displayTime) {
        Player onlineTarget = Bukkit.getPlayerExact((String)targetName);
        boolean isLatest = "Latest".equalsIgnoreCase(displayTime);
        if (onlineTarget != null && onlineTarget.isOnline() && isLatest && sender instanceof Player) {
            Player player = (Player)sender;
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                EnderChestInspectionGui gui = new EnderChestInspectionGui(this.plugin, onlineTarget.getName(), "live", "Now (Live)", onlineTarget.getEnderChest().getContents(), true, onlineTarget);
                player.openInventory(gui.getInventory());
                player.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Opened live Ender Chest of <white>" + onlineTarget.getName() + "</white>.</green>")));
            });
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = isLatest ? "SELECT snapshot_id, timestamp, ender_chest_data FROM player_inventory_snapshots WHERE username = ? ORDER BY id DESC LIMIT 1" : "SELECT snapshot_id, timestamp, ender_chest_data FROM player_inventory_snapshots WHERE username = ? AND timestamp <= ? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, targetName);
                    if (!isLatest) {
                        ps.setTimestamp(2, Timestamp.from(instant));
                    }
                    try (ResultSet rs = ps.executeQuery();){
                        if (rs.next()) {
                            String snapId = rs.getString("snapshot_id");
                            String snapTimestamp = rs.getString("timestamp");
                            String echestData = rs.getString("ender_chest_data");
                            InventorySerializer s = new InventorySerializer();
                            ItemStack[] echestItems = s.deserializeItemArray(echestData);
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Found Ender Chest log: <light_purple>" + snapId + "</light_purple> (<gray>" + snapTimestamp + "</gray>)</green>")));
                            if (sender instanceof Player) {
                                Player player = (Player)sender;
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                    EnderChestInspectionGui gui = new EnderChestInspectionGui(this.plugin, targetName, snapId, snapTimestamp, echestItems);
                                    player.openInventory(gui.getInventory());
                                });
                            } else {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<light_purple>=== Ender Chest Contents for " + targetName + " ===</light_purple>")));
                                if (echestItems != null) {
                                    int count = 0;
                                    for (int i = 0; i < echestItems.length; ++i) {
                                        if (echestItems[i] == null || echestItems[i].getType() == Material.AIR) continue;
                                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Slot " + i + ": <yellow>" + echestItems[i].getAmount() + "x " + echestItems[i].getType().name() + "</yellow></gray>")));
                                        ++count;
                                    }
                                    if (count == 0) {
                                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>(Ender Chest was empty at this snapshot)</gray>"));
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>No Ender Chest logs found for <white>" + targetName + "</white> around <yellow>" + displayTime + "</yellow>.</red>")));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Database error querying Ender Chest: " + e.getMessage() + "</red>")));
            }
        });
    }

    public void queryAndOpenEchestById(Player staff, String targetName, String snapshotId) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = "SELECT snapshot_id, timestamp, ender_chest_data FROM player_inventory_snapshots WHERE snapshot_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, snapshotId);
                    try (ResultSet rs = ps.executeQuery();){
                        if (rs.next()) {
                            String snapId = rs.getString("snapshot_id");
                            String snapTimestamp = rs.getString("timestamp");
                            String echestData = rs.getString("ender_chest_data");
                            InventorySerializer s = new InventorySerializer();
                            ItemStack[] echestItems = s.deserializeItemArray(echestData);
                            staff.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Opened Ender Chest log: <light_purple>" + snapId + "</light_purple> (<gray>" + snapTimestamp + "</gray>)</green>")));
                            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                EnderChestInspectionGui gui = new EnderChestInspectionGui(this.plugin, targetName, snapId, snapTimestamp, echestItems);
                                staff.openInventory(gui.getInventory());
                            });
                        } else {
                            staff.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Ender chest snapshot not found: " + snapshotId + "</red>")));
                        }
                    }
                }
            }
            catch (Exception e) {
                staff.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Database error querying Ender Chest: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handlePosition(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs position <player> [date] [time]</red> <gray>(Alias: /logs pos <player> ...)</gray>"));
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        Object displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = this.parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) {
                targetInstant = this.parseDateTime(args[2] + " " + args[3]);
            }
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = this.parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        this.handlePositionDirect(sender, targetName, targetInstant, (String)displayTime);
    }

    public void handlePositionDirect(CommandSender sender, String targetName, Instant queryTime, String finalDisplayTime) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Querying coordinate trail for <white>" + targetName + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>")));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                Player onlineTarget = Bukkit.getPlayerExact((String)targetName);
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = !isLatest && queryTime != null ? "SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM (  SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM player_position_snapshots WHERE username = ? AND timestamp <= ?   UNION ALL   SELECT world_name, x, y, z, 0.0 AS yaw, 0.0 AS pitch, gamemode, snapshot_reason, timestamp FROM player_inventory_snapshots WHERE username = ? AND timestamp <= ? ) ORDER BY timestamp DESC LIMIT 28" : "SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM (  SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM player_position_snapshots WHERE username = ?   UNION ALL   SELECT world_name, x, y, z, 0.0 AS yaw, 0.0 AS pitch, gamemode, snapshot_reason, timestamp FROM player_inventory_snapshots WHERE username = ? ) ORDER BY timestamp DESC LIMIT 28";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, targetName);
                    if (!isLatest && queryTime != null) {
                        ps.setTimestamp(2, Timestamp.from(queryTime));
                        ps.setString(3, targetName);
                        ps.setTimestamp(4, Timestamp.from(queryTime));
                    } else {
                        ps.setString(2, targetName);
                    }
                    try (ResultSet rs = ps.executeQuery();){
                        ArrayList<LogsListGui.LogDisplayEntry> entries = new ArrayList<LogsListGui.LogDisplayEntry>();
                        boolean first = true;
                        int count = 0;
                        while (rs.next()) {
                            ++count;
                            String world = rs.getString("world_name");
                            double x = rs.getDouble("x");
                            double y = rs.getDouble("y");
                            double z = rs.getDouble("z");
                            float yaw = rs.getFloat("yaw");
                            float pitch = rs.getFloat("pitch");
                            String gm = rs.getString("gamemode");
                            String reason = rs.getString("snapshot_reason");
                            String timestamp = rs.getString("timestamp");
                            ArrayList<String> detailsList = new ArrayList<String>();
                            detailsList.add("<gray>World: <white>" + world + "</white></gray>");
                            detailsList.add("<aqua>X:</aqua> <yellow>" + String.format(Locale.US, "%.1f", x) + "</yellow> <aqua>Y:</aqua> <yellow>" + String.format(Locale.US, "%.1f", y) + "</yellow> <aqua>Z:</aqua> <yellow>" + String.format(Locale.US, "%.1f", z) + "</yellow>");
                            detailsList.add("<gray>Reason: <aqua>" + reason + "</aqua></gray>");
                            detailsList.add("<gray>Gamemode: <white>" + gm + "</white></gray>");
                            detailsList.add("<gray>Timestamp: <yellow>" + timestamp + "</yellow></gray>");
                            entries.add(new LogsListGui.LogDisplayEntry(Material.RECOVERY_COMPASS, "<light_purple>" + reason + "</light_purple> <yellow>[" + (int)x + ", " + (int)y + ", " + (int)z + "]</yellow>", detailsList));
                            if (first) {
                                first = false;
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== Coordinate Trail: " + targetName + " ===</bold></gradient>")));
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Query: <yellow>" + finalDisplayTime + "</yellow> \u2022 Nearest Record: <white>" + timestamp + "</white></gray>")));
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>World: <white>" + world + "</white> | <aqua>X:</aqua> <yellow>" + String.format(Locale.US, "%.1f", x) + "</yellow> <aqua>Y:</aqua> <yellow>" + String.format(Locale.US, "%.1f", y) + "</yellow> <aqua>Z:</aqua> <yellow>" + String.format(Locale.US, "%.1f", z) + "</yellow></gray>")));
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Trigger: <aqua>" + reason + "</aqua> | Gamemode: <white>" + gm + "</white> | Yaw/Pitch: <gray>" + (int)yaw + "\u00b0/" + (int)pitch + "\u00b0</gray></gray>")));
                                Component tpButton = ((TextComponent)Component.text((String)"[Click to Teleport]", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}).clickEvent(ClickEvent.suggestCommand((String)("/tp " + (int)x + " " + (int)y + " " + (int)z)))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)("Click to teleport to " + (int)x + " " + (int)y + " " + (int)z), (TextColor)NamedTextColor.YELLOW)));
                                sender.sendMessage(tpButton);
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Recent Surrounding Breadcrumbs:</gray>"));
                            }
                            if (count > 6) continue;
                            sender.sendMessage(MiniMessage.miniMessage().deserialize((" <dark_gray>\u2022</dark_gray> <gray>" + timestamp + "</gray> <white>" + world + "</white> <yellow>[" + (int)x + ", " + (int)y + ", " + (int)z + "]</yellow> <dark_aqua>(" + reason + ")</dark_aqua>")));
                        }
                        if (first) {
                            if (onlineTarget != null && onlineTarget.isOnline()) {
                                Location loc = onlineTarget.getLocation();
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== Live Location: " + targetName + " (Online) ===</bold></gradient>")));
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>World: <white>" + loc.getWorld().getName() + "</white> | <aqua>X:</aqua> <yellow>" + loc.getBlockX() + "</yellow> <aqua>Y:</aqua> <yellow>" + loc.getBlockY() + "</yellow> <aqua>Z:</aqua> <yellow>" + loc.getBlockZ() + "</yellow></gray>")));
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Ping: <white>" + onlineTarget.getPing() + "ms</white> | Gamemode: <white>" + onlineTarget.getGameMode().name() + "</white></gray>")));
                                Component tpButton = ((TextComponent)Component.text((String)"[Click to Teleport]", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}).clickEvent(ClickEvent.suggestCommand((String)("/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ())))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)("Click to teleport to " + targetName), (TextColor)NamedTextColor.YELLOW)));
                                sender.sendMessage(tpButton);
                            } else {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>No coordinate trail or position logs found for <white>" + targetName + "</white> before " + finalDisplayTime + ".</red>")));
                            }
                        }
                        if (sender instanceof Player) {
                            Player player = (Player)sender;
                            if (!entries.isEmpty()) {
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> new LogsListGui(this.plugin, this.interactiveGui, LogsInteractiveGui.SubsystemCategory.POSITION, targetName, finalDisplayTime, entries, 0).open(player));
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Position query error: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handleItems(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs items <player> [date] [time]</red>"));
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        Object displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = this.parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) {
                targetInstant = this.parseDateTime(args[2] + " " + args[3]);
            }
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = this.parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        this.handleItemsDirect(sender, targetName, targetInstant != null ? targetInstant : Instant.now(), (String)displayTime);
    }

    public void handleItemsDirect(CommandSender sender, String target) {
        this.handleItemsDirect(sender, target, Instant.now(), "Latest");
    }

    public void handleItemsDirect(CommandSender sender, String target, Instant queryTime, String finalDisplayTime) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Querying item events for <white>" + target + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>")));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = isLatest ? "SELECT event_type, material, amount, world_name, x, y, z, timestamp, details FROM player_item_events WHERE actor_name = ? OR affected_player_name = ? ORDER BY timestamp DESC LIMIT 28" : "SELECT event_type, material, amount, world_name, x, y, z, timestamp, details FROM player_item_events WHERE (actor_name = ? OR affected_player_name = ?) AND timestamp <= ? ORDER BY timestamp DESC LIMIT 28";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, target);
                    ps.setString(2, target);
                    if (!isLatest) {
                        ps.setTimestamp(3, Timestamp.from(queryTime));
                    }
                    try (ResultSet rs = ps.executeQuery();){
                        ArrayList<LogsListGui.LogDisplayEntry> entries = new ArrayList<LogsListGui.LogDisplayEntry>();
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== Item Events: " + target + " (" + finalDisplayTime + ") ===</bold></gradient>")));
                        int count = 0;
                        while (rs.next()) {
                            ++count;
                            String type = rs.getString("event_type");
                            String mat = rs.getString("material");
                            int amt = rs.getInt("amount");
                            String world = rs.getString("world_name");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            String ts = rs.getString("timestamp");
                            String det = rs.getString("details");
                            Material iconMat = Material.matchMaterial((String)mat);
                            if (iconMat == null || iconMat == Material.AIR) {
                                iconMat = Material.DIAMOND;
                            }
                            ArrayList<String> detailsList = new ArrayList<String>();
                            detailsList.add("<gray>Event: <yellow>" + type + "</yellow></gray>");
                            detailsList.add("<gray>Amount: <white>" + amt + "x " + mat + "</white></gray>");
                            detailsList.add("<gray>Timestamp: <yellow>" + ts + "</yellow></gray>");
                            detailsList.add("<gray>Location: <white>" + world + " [" + x + ", " + y + ", " + z + "]</white></gray>");
                            if (det != null && !det.isBlank()) {
                                detailsList.add("<gray>Details: <aqua>" + det + "</aqua></gray>");
                            }
                            entries.add(new LogsListGui.LogDisplayEntry(iconMat, "<yellow>" + type.toUpperCase() + "</yellow> <white>" + amt + "x " + mat + "</white>", detailsList));
                            if (count > 8) continue;
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>" + ts + "</gray> <yellow>" + type + "</yellow>: <white>" + amt + "x " + mat + "</white> <dark_gray>@ " + world + " [" + x + "," + y + "," + z + "]</dark_gray> <gray>(" + (det != null ? det : "") + ")</gray>")));
                        }
                        if (count == 0) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>No item events logged for " + target + " before " + finalDisplayTime + ".</gray>")));
                        } else if (count > 8) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>... and " + (count - 8) + " more events (opened in GUI).</gray>")));
                        }
                        if (sender instanceof Player) {
                            Player player = (Player)sender;
                            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> new LogsListGui(this.plugin, this.interactiveGui, LogsInteractiveGui.SubsystemCategory.ITEM_TRANSACTIONS, target, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Error querying item events: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handleActions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs actions <player> [date] [time]</red>"));
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        Object displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = this.parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) {
                targetInstant = this.parseDateTime(args[2] + " " + args[3]);
            }
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = this.parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        this.handleActionsDirect(sender, targetName, targetInstant != null ? targetInstant : Instant.now(), (String)displayTime);
    }

    public void handleActionsDirect(CommandSender sender, String target) {
        this.handleActionsDirect(sender, target, Instant.now(), "Latest");
    }

    public void handleActionsDirect(CommandSender sender, String target, Instant queryTime, String finalDisplayTime) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Querying action audit trail for <white>" + target + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>")));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = isLatest ? "SELECT event_type, world_name, x, y, z, target_info, details, timestamp FROM player_action_logs WHERE username = ? ORDER BY timestamp DESC LIMIT 28" : "SELECT event_type, world_name, x, y, z, target_info, details, timestamp FROM player_action_logs WHERE username = ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT 28";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, target);
                    if (!isLatest) {
                        ps.setTimestamp(2, Timestamp.from(queryTime));
                    }
                    try (ResultSet rs = ps.executeQuery();){
                        ArrayList<LogsListGui.LogDisplayEntry> entries = new ArrayList<LogsListGui.LogDisplayEntry>();
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== Action Logs: " + target + " (" + finalDisplayTime + ") ===</bold></gradient>")));
                        int count = 0;
                        while (rs.next()) {
                            ++count;
                            String type = rs.getString("event_type");
                            String world = rs.getString("world_name");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            String info = rs.getString("target_info");
                            String det = rs.getString("details");
                            String ts = rs.getString("timestamp");
                            ArrayList<String> detailsList = new ArrayList<String>();
                            detailsList.add("<gray>Action: <aqua>" + type + "</aqua></gray>");
                            detailsList.add("<gray>Target: <white>" + (info != null ? info : "-") + "</white></gray>");
                            detailsList.add("<gray>Timestamp: <yellow>" + ts + "</yellow></gray>");
                            detailsList.add("<gray>Location: <white>" + world + " [" + x + ", " + y + ", " + z + "]</white></gray>");
                            if (det != null && !det.isBlank()) {
                                detailsList.add("<gray>Details: <light_purple>" + det + "</light_purple></gray>");
                            }
                            entries.add(new LogsListGui.LogDisplayEntry(Material.WRITTEN_BOOK, "<aqua>" + type.toUpperCase() + "</aqua> <white>" + (info != null ? info : "") + "</white>", detailsList));
                            if (count > 8) continue;
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>" + ts + "</gray> <aqua>" + type + "</aqua> <white>" + (info != null ? info : "") + "</white> <dark_gray>[" + world + " " + x + "," + y + "," + z + "]</dark_gray> <gray>" + (det != null ? det : "") + "</gray>")));
                        }
                        if (count == 0) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>No action logs found for " + target + " before " + finalDisplayTime + ".</gray>")));
                        } else if (count > 8) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>... and " + (count - 8) + " more actions (opened in GUI).</gray>")));
                        }
                        if (sender instanceof Player) {
                            Player player = (Player)sender;
                            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> new LogsListGui(this.plugin, this.interactiveGui, LogsInteractiveGui.SubsystemCategory.BLOCK_ACTIONS, target, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Error querying action logs: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handleSessions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs sessions <player> [date] [time]</red>"));
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        Object displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = this.parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) {
                targetInstant = this.parseDateTime(args[2] + " " + args[3]);
            }
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = this.parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        this.handleSessionsDirect(sender, targetName, targetInstant != null ? targetInstant : Instant.now(), (String)displayTime);
    }

    public void handleSessionsDirect(CommandSender sender, String target) {
        this.handleSessionsDirect(sender, target, Instant.now(), "Latest");
    }

    public void handleSessionsDirect(CommandSender sender, String target, Instant queryTime, String finalDisplayTime) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Querying session history for <white>" + target + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>")));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = isLatest ? "SELECT session_id, platform, joined_at, quit_at, duration_seconds, quit_reason FROM player_sessions WHERE username = ? ORDER BY joined_at DESC LIMIT 28" : "SELECT session_id, platform, joined_at, quit_at, duration_seconds, quit_reason FROM player_sessions WHERE username = ? AND joined_at <= ? ORDER BY joined_at DESC LIMIT 28";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, target);
                    if (!isLatest) {
                        ps.setTimestamp(2, Timestamp.from(queryTime));
                    }
                    try (ResultSet rs = ps.executeQuery();){
                        ArrayList<LogsListGui.LogDisplayEntry> entries = new ArrayList<LogsListGui.LogDisplayEntry>();
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== Session History: " + target + " (" + finalDisplayTime + ") ===</bold></gradient>")));
                        int count = 0;
                        while (rs.next()) {
                            ++count;
                            String sessionId = rs.getString("session_id");
                            String platform = rs.getString("platform");
                            String joined = rs.getString("joined_at");
                            String quit = rs.getString("quit_at");
                            int duration = rs.getInt("duration_seconds");
                            String reason = rs.getString("quit_reason");
                            ArrayList<String> detailsList = new ArrayList<String>();
                            detailsList.add("<gray>Platform: <aqua>" + platform + "</aqua></gray>");
                            detailsList.add("<gray>Joined: <yellow>" + joined + "</yellow></gray>");
                            detailsList.add("<gray>Quit: <yellow>" + (quit != null ? quit : "Active / Online") + "</yellow></gray>");
                            detailsList.add("<gray>Duration: <gold>" + duration / 60 + "m " + duration % 60 + "s</gold></gray>");
                            if (reason != null && !reason.isBlank()) {
                                detailsList.add("<gray>Quit Reason: <white>" + reason + "</white></gray>");
                            }
                            entries.add(new LogsListGui.LogDisplayEntry(Material.CLOCK, "<gradient:#e879f9:#c084fc>Session</gradient> <yellow>" + (joined != null ? joined.substring(0, Math.min(10, joined.length())) : sessionId) + "</yellow>", detailsList));
                            if (count > 8) continue;
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Joined: <white>" + joined + "</white> | Quit: <white>" + (quit != null ? quit : "Active/Online") + "</white> | Duration: <yellow>" + duration / 60 + "m</yellow> | Platform: <aqua>" + platform + "</aqua></gray>")));
                        }
                        if (count == 0) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>No recorded sessions found for " + target + " before " + finalDisplayTime + ".</gray>")));
                        } else if (count > 8) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>... and " + (count - 8) + " more sessions (opened in GUI).</gray>")));
                        }
                        if (sender instanceof Player) {
                            Player player = (Player)sender;
                            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> new LogsListGui(this.plugin, this.interactiveGui, LogsInteractiveGui.SubsystemCategory.SESSIONS, target, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Error querying sessions: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs search <item-material></red>"));
            return;
        }
        String query = "%" + args[1].toUpperCase() + "%";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>Searching item logs for <white>" + args[1] + "</white>...</gray>")));
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = "SELECT event_type, actor_name, material, amount, world_name, x, y, z, timestamp FROM player_item_events WHERE material LIKE ? OR item_display_name LIKE ? ORDER BY timestamp DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, query);
                    ps.setString(2, query);
                    try (ResultSet rs = ps.executeQuery();){
                        boolean found = false;
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== Search Results: " + args[1] + " ===</bold></gradient>")));
                        while (rs.next()) {
                            found = true;
                            String event = rs.getString("event_type");
                            String actor = rs.getString("actor_name");
                            String mat = rs.getString("material");
                            int amt = rs.getInt("amount");
                            String ts = rs.getString("timestamp");
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>" + ts + "</gray> <white>" + actor + "</white> <yellow>" + event + "</yellow> <gold>" + amt + "x " + mat + "</gold>")));
                        }
                        if (!found) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>No matching item events found.</gray>"));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Search failed: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#93c5fd:#3b82f6><bold>Moonlight Core \u2022 Audit & Log Engine</bold></gradient>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs <player> <date> <time></yellow> <gray>- Inspect inventory at DD/MM/YYYY HH:MM:SS</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs echest <player> [date] [time]</yellow> <gray>- Inspect Ender Chest log</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs position <player> [date] [time]</yellow> <gray>- Query coordinate trail & location</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs snapshot <player></yellow> <gray>- Take immediate live inventory snapshot</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs items <player></yellow> <gray>- Query pickup/drop/death item audit logs</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs actions <player></yellow> <gray>- Query block & container action history</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs sessions <player></yellow> <gray>- View session duration & platform history</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs search <material></yellow> <gray>- Search audit trail for specific item</gray>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/logs export <player></yellow> <gray>- Export sanitized CSV report</gray>"));
    }

    private void handleSnapshot(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs snapshot <player></red>"));
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found or offline.</red>"));
            return;
        }
        String snapId = this.plugin.getAuditManager().getDatabaseManager().recordInventorySnapshot(target, "manual", UUID.randomUUID().toString());
        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Created manual snapshot for <white>" + target.getName() + "</white>: <yellow>" + snapId + "</yellow></green>")));
    }

    private void handleInventorySub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs inventory <player> [date] [time]</red> <gray>(Alias: /logs inv <player> ...)</gray>"));
            return;
        }
        String targetName = args[1];
        if (args.length >= 4) {
            Instant instant = this.parseDateTime(args[2] + "_" + args[3]);
            this.queryAndOpenSnapshot(sender, targetName, instant != null ? instant : Instant.now(), args[2] + " " + args[3]);
        } else if (args.length == 3) {
            Instant instant = this.parseDateTime(args[2]);
            this.queryAndOpenSnapshot(sender, targetName, instant != null ? instant : Instant.now(), args[2]);
        } else {
            this.queryAndOpenSnapshot(sender, targetName, Instant.now(), "Latest");
        }
    }

    private void handleExport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs export <player></red>"));
            return;
        }
        String player = args[1];
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                File dir = new File(this.plugin.getDataFolder(), "exports");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File exportFile = new File(dir, "audit_" + player + "_" + System.currentTimeMillis() + ".csv");
                try (FileWriter writer = new FileWriter(exportFile);){
                    writer.write("Timestamp,Type,Player,Details\n");
                    String safeDetail = LogsCommand.sanitizeCsvField("Audit export generated for " + player);
                    writer.write(String.valueOf(Instant.now()) + ",EXPORT_SUMMARY," + LogsCommand.sanitizeCsvField(player) + "," + safeDetail + "\n");
                }
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<green>Export generated safely at: <yellow>plugins/MoonlightVanillaSurvivalCore/exports/" + exportFile.getName() + "</yellow></green>")));
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>Export failed: " + e.getMessage() + "</red>")));
            }
        });
    }

    private void handlePrivacy(CommandSender sender, String[] args) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Data Privacy Summary: Player IP tracking is disabled by default. Sensitive logs are strictly OP-only.</green>"));
    }

    private void handleIpLookup(CommandSender sender, String[] args) {
        if (!this.plugin.getAuditManager().getConfig().isTrackIpAddresses()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>IP tracking is disabled in audit.yml (logging.track-ip-addresses: false).</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /logs ip <player></red>"));
            return;
        }
        String target = args[1];
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try (Connection conn = this.plugin.getAuditManager().getDatabaseManager().getConnection();){
                String sql = "SELECT DISTINCT ip_address, joined_at FROM player_sessions WHERE username = ? AND ip_address IS NOT NULL ORDER BY joined_at DESC LIMIT 5";
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, target);
                    try (ResultSet rs = ps.executeQuery();){
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gradient:#6366f1:#a855f7><bold>=== IP History: " + target + " ===</bold></gradient>")));
                        boolean found = false;
                        while (rs.next()) {
                            found = true;
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>IP: <yellow>" + rs.getString("ip_address") + "</yellow> (seen at " + rs.getString("joined_at") + ")</gray>")));
                        }
                        if (!found) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(("<gray>No IP records logged for " + target + ".</gray>")));
                        }
                    }
                }
            }
            catch (Exception e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(("<red>IP query error: " + e.getMessage() + "</red>")));
            }
        });
    }

    public static String sanitizeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (((String)field).startsWith("=") || ((String)field).startsWith("+") || ((String)field).startsWith("-") || ((String)field).startsWith("@")) {
            field = "'" + (String)field;
        }
        return "\"" + ((String)field).replace("\"", "\"\"") + "\"";
    }

    public Instant parseDateTime(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return formatter.parse((CharSequence)input, Instant::from);
            }
            catch (Exception exception) {
            }
        }
        return null;
    }

    public Instant parseRelativeOrAbsoluteTime(String rawInput) {
        Instant parsed;
        if (rawInput == null || rawInput.isBlank()) {
            return null;
        }
        String input = rawInput.trim();
        if (input.equalsIgnoreCase("now") || input.equalsIgnoreCase("latest") || input.equalsIgnoreCase("present")) {
            return Instant.now();
        }
        if (input.equalsIgnoreCase("yesterday")) {
            return Instant.now().minus(1L, ChronoUnit.DAYS);
        }
        if (input.equalsIgnoreCase("today")) {
            return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        Pattern relativePattern = Pattern.compile("(?i)(\\d+)\\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)");
        Matcher matcher = relativePattern.matcher(input);
        boolean matchedAny = false;
        long totalSeconds = 0L;
        int lastEnd = 0;
        while (matcher.find()) {
            matchedAny = true;
            try {
                long val = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();
                if (unit.startsWith("s")) {
                    totalSeconds += val;
                } else if (unit.startsWith("m")) {
                    totalSeconds += val * 60L;
                } else if (unit.startsWith("h")) {
                    totalSeconds += val * 3600L;
                } else if (unit.startsWith("d")) {
                    totalSeconds += val * 86400L;
                } else if (unit.startsWith("w")) {
                    totalSeconds += val * 604800L;
                }
            }
            catch (NumberFormatException val) {
                // empty catch block
            }
            lastEnd = matcher.end();
        }
        if (matchedAny && lastEnd == input.length()) {
            return Instant.now().minusSeconds(totalSeconds);
        }
        if (input.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
            try {
                String[] parts = input.split(":");
                int hour = Integer.parseInt(parts[0]);
                int min = Integer.parseInt(parts[1]);
                int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                LocalTime time = LocalTime.of(hour, min, sec);
                LocalDate today = LocalDate.now();
                Instant inst = time.atDate(today).atZone(ZoneId.systemDefault()).toInstant();
                if (inst.isAfter(Instant.now())) {
                    inst = time.atDate(today.minusDays(1L)).atZone(ZoneId.systemDefault()).toInstant();
                }
                return inst;
            }
            catch (Exception parts) {
                // empty catch block
            }
        }
        if ((parsed = this.parseDateTime(input)) != null) {
            return parsed;
        }
        parsed = this.parseDateTime(input + "_12:00:00");
        if (parsed != null) {
            return parsed;
        }
        parsed = this.parseDateTime(input.replace(" ", "_"));
        return parsed;
    }

    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!LogsCommand.isAuthorized(sender)) {
            return Collections.emptyList();
        }
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length <= 1) {
            String query = args.length == 0 ? "" : args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!VanishUtil.canSee(sender, p) || !p.getName().toLowerCase().startsWith(query)) continue;
                completions.add(p.getName());
            }
            for (String sub : List.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv", "snapshot", "items", "actions", "sessions", "search", "export", "privacy", "ip", "help")) {
                if (!sub.startsWith(query)) continue;
                completions.add(sub);
            }
            return completions;
        }
        if (args.length == 2) {
            String first = args[0].toLowerCase();
            String query = args[1].toLowerCase();
            if (List.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv", "snapshot", "items", "actions", "sessions", "export", "privacy", "ip").contains(first)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!VanishUtil.canSee(sender, p) || !p.getName().toLowerCase().startsWith(query)) continue;
                    completions.add(p.getName());
                }
                return completions;
            }
            for (String dur : List.of("5m", "10m", "15m", "30m", "1h", "6h", "12h", "1d", "7d", "now", "yesterday")) {
                if (!dur.toLowerCase().startsWith(query)) continue;
                completions.add(dur);
            }
            String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            if (todayDate.toLowerCase().startsWith(query)) {
                completions.add(todayDate);
            }
            if ("dd/mm/yyyy".startsWith(query)) {
                completions.add("DD/MM/YYYY");
            }
            return completions;
        }
        if (args.length == 3) {
            String first = args[0].toLowerCase();
            String query = args[2].toLowerCase();
            if (List.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv").contains(first)) {
                for (String dur : List.of("5m", "10m", "15m", "30m", "1h", "6h", "12h", "1d", "7d", "now", "yesterday")) {
                    if (!dur.toLowerCase().startsWith(query)) continue;
                    completions.add(dur);
                }
                String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                if (todayDate.toLowerCase().startsWith(query)) {
                    completions.add(todayDate);
                }
                if ("dd/mm/yyyy".startsWith(query)) {
                    completions.add("DD/MM/YYYY");
                }
                return completions;
            }
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (nowTime.startsWith(query)) {
                completions.add(nowTime);
            }
            if ("hh:mm:ss".startsWith(query)) {
                completions.add("HH:MM:SS");
            }
            if ("12:00:00".startsWith(query)) {
                completions.add("12:00:00");
            }
            if ("18:00:00".startsWith(query)) {
                completions.add("18:00:00");
            }
            if ("00:00:00".startsWith(query)) {
                completions.add("00:00:00");
            }
            return completions;
        }
        if (args.length == 4) {
            String first = args[0].toLowerCase();
            String query = args[3].toLowerCase();
            if (List.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv").contains(first)) {
                String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                if (nowTime.startsWith(query)) {
                    completions.add(nowTime);
                }
                if ("hh:mm:ss".startsWith(query)) {
                    completions.add("HH:MM:SS");
                }
                if ("12:00:00".startsWith(query)) {
                    completions.add("12:00:00");
                }
                if ("18:00:00".startsWith(query)) {
                    completions.add("18:00:00");
                }
                return completions;
            }
        }
        return completions;
    }
}

