package com.moonlight.vanilla.modules.audit.commands;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.audit.gui.EnderChestInspectionGui;
import com.moonlight.vanilla.modules.audit.gui.LogsInteractiveGui;
import com.moonlight.vanilla.modules.audit.gui.LogsListGui;
import com.moonlight.vanilla.modules.audit.gui.SnapshotInspectionGui;
import com.moonlight.vanilla.modules.audit.serialization.InventorySerializer;
import com.moonlight.vanilla.utils.VanishUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogsCommand (/logs, /auditlogs, /paperlogs, /mlogs)
 * Comprehensive staff audit inspection, point-in-time inventory/echest recovery,
 * coordinate breadcrumbs, item transaction tracking, and interactive GUI launcher.
 */
public class LogsCommand implements CommandExecutor, TabCompleter {

    private final MoonlightVanillaSurvivalCore plugin;
    private final LogsInteractiveGui interactiveGui;

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd/MM/yyyy_HH:mm:ss").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()),
            DateTimeFormatter.ISO_INSTANT
    };

    public LogsCommand(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
        this.interactiveGui = new LogsInteractiveGui(plugin, this);
    }

    public LogsInteractiveGui getInteractiveGui() {
        return interactiveGui;
    }

    public static boolean isAuthorized(CommandSender sender) {
        return !(sender instanceof Player p) || p.isOp() || p.hasPermission("moonlight.logs.view") || p.hasPermission("moonlight.core.admin");
    }

    private static void msg(CommandSender sender, String miniMessageText) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(miniMessageText));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isAuthorized(sender)) {
            String deniedMsg = plugin.getAuditManager() != null ? plugin.getAuditManager().getConfig().getNoPermissionMessage()
                    : "<red>Access denied: You must be a staff member (Trial Helper+) or server operator (OP) to use audit logging tools.</red>";
            msg(sender, deniedMsg);
            return true;
        }

        if (sender instanceof Player player && (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu"))) {
            interactiveGui.openStage1(player);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String firstArg = args[0].toLowerCase(Locale.ROOT);
        if (sender instanceof Player player && firstArg.equals("cancelprompt")) {
            interactiveGui.cancelPrompt(player);
            return true;
        }

        switch (firstArg) {
            case "snapshot" -> handleSnapshot(sender, args);
            case "inventory", "inv" -> handleInventorySub(sender, args);
            case "echest", "ec", "enderchest" -> handleEchestSub(sender, args);
            case "position", "pos" -> handlePosition(sender, args);
            case "items" -> handleItems(sender, args);
            case "actions" -> handleActions(sender, args);
            case "sessions" -> handleSessions(sender, args);
            case "search" -> handleSearch(sender, args);
            case "export" -> handleExport(sender, args);
            case "privacy" -> handlePrivacy(sender, args);
            case "ip" -> handleIpLookup(sender, args);
            default -> handlePlayerDateQuery(sender, args);
        }
        return true;
    }

    private void handlePlayerDateQuery(CommandSender sender, String[] args) {
        String targetName = args[0];
        if (args.length >= 3) {
            String combined = args[1] + " " + args[2];
            Instant targetInstant = parseDateTime(args[1] + "_" + args[2]);
            if (targetInstant == null) targetInstant = parseDateTime(combined);
            if (targetInstant == null) targetInstant = parseRelativeOrAbsoluteTime(combined);
            if (targetInstant == null) targetInstant = parseRelativeOrAbsoluteTime(args[1]);

            if (targetInstant == null) {
                msg(sender, "<red>Invalid Date/Time format! Use: <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow>, or <yellow>DD/MM/YYYY HH:MM:SS</yellow>.</red>");
                return;
            }
            queryAndOpenSnapshot(sender, targetName, targetInstant, combined);
            return;
        }

        if (args.length == 2) {
            Instant targetInstant = parseRelativeOrAbsoluteTime(args[1]);
            if (targetInstant != null) {
                queryAndOpenSnapshot(sender, targetName, targetInstant, args[1]);
            } else {
                msg(sender, "<red>Invalid Date or Duration format! Use: <yellow>10m</yellow>, <yellow>1h</yellow>, <yellow>1d</yellow>, <yellow>15:30:00</yellow>, or <yellow>DD/MM/YYYY</yellow>.</red>");
            }
            return;
        }

        msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Player Logs & Activity Overview ===</bold></gradient>");
        msg(sender, "<gray>Target: <white>" + targetName + "</white></gray>");
        msg(sender, "<gray>Syntax: <yellow>/logs " + targetName + " <DD/MM/YYYY> <HH:MM:SS></yellow></gray>");
        msg(sender, "<gray>Fetching most recent snapshot...</gray>");
        queryAndOpenSnapshot(sender, targetName, Instant.now(), "Latest");
    }

    public void queryAndOpenSnapshot(CommandSender sender, String targetName, Instant instant, String displayTime) {
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        boolean isLatest = "Latest".equalsIgnoreCase(displayTime);
        if (onlineTarget != null && onlineTarget.isOnline() && isLatest && sender instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                SnapshotInspectionGui gui = new SnapshotInspectionGui(plugin, onlineTarget.getName(), "live", "Now (Live)",
                        onlineTarget.getInventory().getStorageContents(), onlineTarget.getInventory().getArmorContents(),
                        onlineTarget.getInventory().getItemInOffHand(), onlineTarget.getLevel(), onlineTarget.getTotalExperience(),
                        onlineTarget.getHealth(), onlineTarget.getFoodLevel(), onlineTarget.getGameMode().name(), true, onlineTarget);
                player.openInventory(gui.getInventory());
                msg(player, "<green>Opened live active inventory of <white>" + onlineTarget.getName() + "</white>.</green>");
            });
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                String sql = isLatest
                        ? "SELECT * FROM player_inventory_snapshots WHERE username = ? ORDER BY id DESC LIMIT 1"
                        : "SELECT * FROM player_inventory_snapshots WHERE username = ? AND timestamp <= ? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetName);
                    if (!isLatest) ps.setTimestamp(2, Timestamp.from(instant));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            loadSnapshotFromResultSet(rs, sender, targetName);
                        } else {
                            msg(sender, "<red>No logs or inventory snapshots found for <white>" + targetName + "</white> around <yellow>" + displayTime + "</yellow>.</red>");
                        }
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Database query error: " + e.getMessage() + "</red>");
            }
        });
    }

    public void queryAndOpenSnapshotById(Player staff, String targetName, String snapshotId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_inventory_snapshots WHERE snapshot_id = ?")) {
                    ps.setString(1, snapshotId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            loadSnapshotFromResultSet(rs, staff, targetName);
                        } else {
                            msg(staff, "<red>Snapshot not found: " + snapshotId + "</red>");
                        }
                    }
                }
            } catch (Exception e) {
                msg(staff, "<red>Database error loading snapshot: " + e.getMessage() + "</red>");
            }
        });
    }

    private void loadSnapshotFromResultSet(ResultSet rs, CommandSender sender, String targetName) throws Exception {
        String snapId = rs.getString("snapshot_id");
        String snapTimestamp = rs.getString("timestamp");
        InventorySerializer s = new InventorySerializer();
        ItemStack[] storage = s.deserializeItemArray(rs.getString("inventory_data"));
        ItemStack[] armor = s.deserializeItemArray(rs.getString("armor_data"));
        ItemStack offhand = s.deserializeItem(rs.getString("offhand_data"));
        int expLevel = rs.getInt("exp_level");
        int totalExp = rs.getInt("total_exp");
        double health = rs.getDouble("health");
        int foodLevel = rs.getInt("hunger");
        String gamemode = rs.getString("gamemode");

        msg(sender, "<green>Found log snapshot: <yellow>" + snapId + "</yellow> (<gray>" + snapTimestamp + "</gray>)</green>");
        if (sender instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                SnapshotInspectionGui gui = new SnapshotInspectionGui(plugin, targetName, snapId, snapTimestamp, storage, armor, offhand, expLevel, totalExp, health, foodLevel, gamemode, false, null);
                player.openInventory(gui.getInventory());
            });
        } else {
            msg(sender, "<gold>=== Inventory Contents for " + targetName + " ===</gold>");
            if (storage != null) {
                for (int i = 0; i < storage.length; ++i) {
                    if (storage[i] != null && storage[i].getType() != Material.AIR) {
                        msg(sender, "<gray>Slot " + i + ": <yellow>" + storage[i].getAmount() + "x " + storage[i].getType().name() + "</yellow></gray>");
                    }
                }
            }
        }
    }

    private void handleEchestSub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs echest <player> [date] [time]</red> <gray>(Alias: /logs ec <player> ...)</gray>");
            return;
        }
        String targetName = args[1];
        if (args.length >= 4) {
            Instant instant = parseDateTime(args[2] + "_" + args[3]);
            if (instant == null) instant = parseDateTime(args[2] + " " + args[3]);
            queryAndOpenEchest(sender, targetName, instant != null ? instant : Instant.now(), args[2] + " " + args[3]);
        } else if (args.length == 3) {
            Instant instant = parseRelativeOrAbsoluteTime(args[2]);
            queryAndOpenEchest(sender, targetName, instant != null ? instant : Instant.now(), args[2]);
        } else {
            queryAndOpenEchest(sender, targetName, Instant.now(), "Latest");
        }
    }

    public void queryAndOpenEchest(CommandSender sender, String targetName, Instant instant, String displayTime) {
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        boolean isLatest = "Latest".equalsIgnoreCase(displayTime);
        if (onlineTarget != null && onlineTarget.isOnline() && isLatest && sender instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                EnderChestInspectionGui gui = new EnderChestInspectionGui(plugin, onlineTarget.getName(), "live", "Now (Live)", onlineTarget.getEnderChest().getContents(), true, onlineTarget);
                player.openInventory(gui.getInventory());
                msg(player, "<green>Opened live Ender Chest of <white>" + onlineTarget.getName() + "</white>.</green>");
            });
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                String sql = isLatest
                        ? "SELECT snapshot_id, timestamp, ender_chest_data FROM player_inventory_snapshots WHERE username = ? ORDER BY id DESC LIMIT 1"
                        : "SELECT snapshot_id, timestamp, ender_chest_data FROM player_inventory_snapshots WHERE username = ? AND timestamp <= ? ORDER BY id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetName);
                    if (!isLatest) ps.setTimestamp(2, Timestamp.from(instant));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            loadEchestFromResultSet(rs, sender, targetName);
                        } else {
                            msg(sender, "<red>No Ender Chest logs found for <white>" + targetName + "</white> around <yellow>" + displayTime + "</yellow>.</red>");
                        }
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Database error querying Ender Chest: " + e.getMessage() + "</red>");
            }
        });
    }

    public void queryAndOpenEchestById(Player staff, String targetName, String snapshotId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT snapshot_id, timestamp, ender_chest_data FROM player_inventory_snapshots WHERE snapshot_id = ?")) {
                    ps.setString(1, snapshotId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            loadEchestFromResultSet(rs, staff, targetName);
                        } else {
                            msg(staff, "<red>Ender chest snapshot not found: " + snapshotId + "</red>");
                        }
                    }
                }
            } catch (Exception e) {
                msg(staff, "<red>Database error querying Ender Chest: " + e.getMessage() + "</red>");
            }
        });
    }

    private void loadEchestFromResultSet(ResultSet rs, CommandSender sender, String targetName) throws Exception {
        String snapId = rs.getString("snapshot_id");
        String snapTimestamp = rs.getString("timestamp");
        ItemStack[] echestItems = new InventorySerializer().deserializeItemArray(rs.getString("ender_chest_data"));
        msg(sender, "<green>Found Ender Chest log: <light_purple>" + snapId + "</light_purple> (<gray>" + snapTimestamp + "</gray>)</green>");

        if (sender instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(new EnderChestInspectionGui(plugin, targetName, snapId, snapTimestamp, echestItems).getInventory()));
        } else {
            msg(sender, "<light_purple>=== Ender Chest Contents for " + targetName + " ===</light_purple>");
            if (echestItems != null) {
                int count = 0;
                for (int i = 0; i < echestItems.length; ++i) {
                    if (echestItems[i] != null && echestItems[i].getType() != Material.AIR) {
                        msg(sender, "<gray>Slot " + i + ": <yellow>" + echestItems[i].getAmount() + "x " + echestItems[i].getType().name() + "</yellow></gray>");
                        count++;
                    }
                }
                if (count == 0) msg(sender, "<gray>(Ender Chest was empty at this snapshot)</gray>");
            }
        }
    }

    private void handlePosition(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs position <player> [date] [time]</red> <gray>(Alias: /logs pos <player> ...)</gray>");
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        String displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) targetInstant = parseDateTime(args[2] + " " + args[3]);
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        handlePositionDirect(sender, targetName, targetInstant, displayTime);
    }

    public void handlePositionDirect(CommandSender sender, String targetName, Instant queryTime, String finalDisplayTime) {
        msg(sender, "<gray>Querying coordinate trail for <white>" + targetName + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                Player onlineTarget = Bukkit.getPlayerExact(targetName);
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = (!isLatest && queryTime != null)
                        ? "SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM ( SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM player_position_snapshots WHERE username = ? AND timestamp <= ? UNION ALL SELECT world_name, x, y, z, 0.0 AS yaw, 0.0 AS pitch, gamemode, snapshot_reason, timestamp FROM player_inventory_snapshots WHERE username = ? AND timestamp <= ? ) ORDER BY timestamp DESC LIMIT 28"
                        : "SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM ( SELECT world_name, x, y, z, yaw, pitch, gamemode, snapshot_reason, timestamp FROM player_position_snapshots WHERE username = ? UNION ALL SELECT world_name, x, y, z, 0.0 AS yaw, 0.0 AS pitch, gamemode, snapshot_reason, timestamp FROM player_inventory_snapshots WHERE username = ? ) ORDER BY timestamp DESC LIMIT 28";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, targetName);
                    if (!isLatest && queryTime != null) {
                        ps.setTimestamp(2, Timestamp.from(queryTime));
                        ps.setString(3, targetName);
                        ps.setTimestamp(4, Timestamp.from(queryTime));
                    } else {
                        ps.setString(2, targetName);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        List<LogsListGui.LogDisplayEntry> entries = new ArrayList<>();
                        boolean first = true;
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            String world = rs.getString("world_name");
                            double x = rs.getDouble("x");
                            double y = rs.getDouble("y");
                            double z = rs.getDouble("z");
                            float yaw = rs.getFloat("yaw");
                            float pitch = rs.getFloat("pitch");
                            String gm = rs.getString("gamemode");
                            String reason = rs.getString("snapshot_reason");
                            String timestamp = rs.getString("timestamp");

                            List<String> details = List.of(
                                    "<gray>World: <white>" + world + "</white></gray>",
                                    "<aqua>X:</aqua> <yellow>" + String.format(Locale.US, "%.1f", x) + "</yellow> <aqua>Y:</aqua> <yellow>" + String.format(Locale.US, "%.1f", y) + "</yellow> <aqua>Z:</aqua> <yellow>" + String.format(Locale.US, "%.1f", z) + "</yellow>",
                                    "<gray>Reason: <aqua>" + reason + "</aqua></gray>",
                                    "<gray>Gamemode: <white>" + gm + "</white></gray>",
                                    "<gray>Timestamp: <yellow>" + timestamp + "</yellow></gray>"
                            );
                            entries.add(new LogsListGui.LogDisplayEntry(Material.RECOVERY_COMPASS, "<light_purple>" + reason + "</light_purple> <yellow>[" + (int) x + ", " + (int) y + ", " + (int) z + "]</yellow>", details));

                            if (first) {
                                first = false;
                                msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Coordinate Trail: " + targetName + " ===</bold></gradient>");
                                msg(sender, "<gray>Query: <yellow>" + finalDisplayTime + "</yellow> • Nearest Record: <white>" + timestamp + "</white></gray>");
                                msg(sender, "<gray>World: <white>" + world + "</white> | <aqua>X:</aqua> <yellow>" + String.format(Locale.US, "%.1f", x) + "</yellow> <aqua>Y:</aqua> <yellow>" + String.format(Locale.US, "%.1f", y) + "</yellow> <aqua>Z:</aqua> <yellow>" + String.format(Locale.US, "%.1f", z) + "</yellow></gray>");
                                msg(sender, "<gray>Trigger: <aqua>" + reason + "</aqua> | Gamemode: <white>" + gm + "</white> | Yaw/Pitch: <gray>" + (int) yaw + "°/" + (int) pitch + "°</gray></gray>");
                                Component tpBtn = Component.text("[Click to Teleport]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                        .clickEvent(ClickEvent.suggestCommand("/tp " + (int) x + " " + (int) y + " " + (int) z))
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + (int) x + " " + (int) y + " " + (int) z, NamedTextColor.YELLOW)));
                                sender.sendMessage(tpBtn);
                                msg(sender, "<gray>Recent Surrounding Breadcrumbs:</gray>");
                            }
                            if (count <= 6) {
                                msg(sender, " <dark_gray>•</dark_gray> <gray>" + timestamp + "</gray> <white>" + world + "</white> <yellow>[" + (int) x + ", " + (int) y + ", " + (int) z + "]</yellow> <dark_aqua>(" + reason + ")</dark_aqua>");
                            }
                        }

                        if (first) {
                            if (onlineTarget != null && onlineTarget.isOnline()) {
                                Location loc = onlineTarget.getLocation();
                                msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Live Location: " + targetName + " (Online) ===</bold></gradient>");
                                msg(sender, "<gray>World: <white>" + loc.getWorld().getName() + "</white> | <aqua>X:</aqua> <yellow>" + loc.getBlockX() + "</yellow> <aqua>Y:</aqua> <yellow>" + loc.getBlockY() + "</yellow> <aqua>Z:</aqua> <yellow>" + loc.getBlockZ() + "</yellow></gray>");
                                msg(sender, "<gray>Ping: <white>" + onlineTarget.getPing() + "ms</white> | Gamemode: <white>" + onlineTarget.getGameMode().name() + "</white></gray>");
                                Component tpBtn = Component.text("[Click to Teleport]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                        .clickEvent(ClickEvent.suggestCommand("/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()))
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + targetName, NamedTextColor.YELLOW)));
                                sender.sendMessage(tpBtn);
                            } else {
                                msg(sender, "<red>No coordinate trail or position logs found for <white>" + targetName + "</white> before " + finalDisplayTime + ".</red>");
                            }
                        }

                        if (sender instanceof Player player && !entries.isEmpty()) {
                            Bukkit.getScheduler().runTask(plugin, () -> new LogsListGui(plugin, interactiveGui, LogsInteractiveGui.SubsystemCategory.POSITION, targetName, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Position query error: " + e.getMessage() + "</red>");
            }
        });
    }

    private void handleItems(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs items <player> [date] [time]</red>");
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        String displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) targetInstant = parseDateTime(args[2] + " " + args[3]);
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        handleItemsDirect(sender, targetName, targetInstant != null ? targetInstant : Instant.now(), displayTime);
    }

    public void handleItemsDirect(CommandSender sender, String target) {
        handleItemsDirect(sender, target, Instant.now(), "Latest");
    }

    public void handleItemsDirect(CommandSender sender, String target, Instant queryTime, String finalDisplayTime) {
        msg(sender, "<gray>Querying item events for <white>" + target + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = isLatest
                        ? "SELECT event_type, material, amount, world_name, x, y, z, timestamp, details FROM player_item_events WHERE actor_name = ? OR affected_player_name = ? ORDER BY timestamp DESC LIMIT 28"
                        : "SELECT event_type, material, amount, world_name, x, y, z, timestamp, details FROM player_item_events WHERE (actor_name = ? OR affected_player_name = ?) AND timestamp <= ? ORDER BY timestamp DESC LIMIT 28";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    ps.setString(2, target);
                    if (!isLatest) ps.setTimestamp(3, Timestamp.from(queryTime));

                    try (ResultSet rs = ps.executeQuery()) {
                        List<LogsListGui.LogDisplayEntry> entries = new ArrayList<>();
                        msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Item Events: " + target + " (" + finalDisplayTime + ") ===</bold></gradient>");
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            String type = rs.getString("event_type");
                            String mat = rs.getString("material");
                            int amt = rs.getInt("amount");
                            String world = rs.getString("world_name");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            String ts = rs.getString("timestamp");
                            String det = rs.getString("details");

                            Material iconMat = Material.matchMaterial(mat);
                            if (iconMat == null || iconMat.isAir()) iconMat = Material.DIAMOND;

                            List<String> details = new ArrayList<>(List.of(
                                    "<gray>Event: <yellow>" + type + "</yellow></gray>",
                                    "<gray>Amount: <white>" + amt + "x " + mat + "</white></gray>",
                                    "<gray>Timestamp: <yellow>" + ts + "</yellow></gray>",
                                    "<gray>Location: <white>" + world + " [" + x + ", " + y + ", " + z + "]</white></gray>"
                            ));
                            if (det != null && !det.isBlank()) details.add("<gray>Details: <aqua>" + det + "</aqua></gray>");

                            entries.add(new LogsListGui.LogDisplayEntry(iconMat, "<yellow>" + type.toUpperCase(Locale.ROOT) + "</yellow> <white>" + amt + "x " + mat + "</white>", details));
                            if (count <= 8) {
                                msg(sender, "<gray>" + ts + "</gray> <yellow>" + type + "</yellow>: <white>" + amt + "x " + mat + "</white> <dark_gray>@ " + world + " [" + x + "," + y + "," + z + "]</dark_gray> <gray>(" + (det != null ? det : "") + ")</gray>");
                            }
                        }

                        if (count == 0) {
                            msg(sender, "<gray>No item events logged for " + target + " before " + finalDisplayTime + ".</gray>");
                        } else if (count > 8) {
                            msg(sender, "<gray>... and " + (count - 8) + " more events (opened in GUI).</gray>");
                        }

                        if (sender instanceof Player player) {
                            Bukkit.getScheduler().runTask(plugin, () -> new LogsListGui(plugin, interactiveGui, LogsInteractiveGui.SubsystemCategory.ITEM_TRANSACTIONS, target, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Error querying item events: " + e.getMessage() + "</red>");
            }
        });
    }

    private void handleActions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs actions <player> [date] [time]</red>");
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        String displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) targetInstant = parseDateTime(args[2] + " " + args[3]);
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        handleActionsDirect(sender, targetName, targetInstant != null ? targetInstant : Instant.now(), displayTime);
    }

    public void handleActionsDirect(CommandSender sender, String target) {
        handleActionsDirect(sender, target, Instant.now(), "Latest");
    }

    public void handleActionsDirect(CommandSender sender, String target, Instant queryTime, String finalDisplayTime) {
        msg(sender, "<gray>Querying action audit trail for <white>" + target + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = isLatest
                        ? "SELECT event_type, world_name, x, y, z, target_info, details, timestamp FROM player_action_logs WHERE username = ? ORDER BY timestamp DESC LIMIT 28"
                        : "SELECT event_type, world_name, x, y, z, target_info, details, timestamp FROM player_action_logs WHERE username = ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT 28";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    if (!isLatest) ps.setTimestamp(2, Timestamp.from(queryTime));

                    try (ResultSet rs = ps.executeQuery()) {
                        List<LogsListGui.LogDisplayEntry> entries = new ArrayList<>();
                        msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Action Logs: " + target + " (" + finalDisplayTime + ") ===</bold></gradient>");
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            String type = rs.getString("event_type");
                            String world = rs.getString("world_name");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            String info = rs.getString("target_info");
                            String det = rs.getString("details");
                            String ts = rs.getString("timestamp");

                            List<String> details = new ArrayList<>(List.of(
                                    "<gray>Action: <aqua>" + type + "</aqua></gray>",
                                    "<gray>Target: <white>" + (info != null ? info : "-") + "</white></gray>",
                                    "<gray>Timestamp: <yellow>" + ts + "</yellow></gray>",
                                    "<gray>Location: <white>" + world + " [" + x + ", " + y + ", " + z + "]</white></gray>"
                            ));
                            if (det != null && !det.isBlank()) details.add("<gray>Details: <light_purple>" + det + "</light_purple></gray>");

                            entries.add(new LogsListGui.LogDisplayEntry(Material.WRITTEN_BOOK, "<aqua>" + type.toUpperCase(Locale.ROOT) + "</aqua> <white>" + (info != null ? info : "") + "</white>", details));
                            if (count <= 8) {
                                msg(sender, "<gray>" + ts + "</gray> <aqua>" + type + "</aqua> <white>" + (info != null ? info : "") + "</white> <dark_gray>[" + world + " " + x + "," + y + "," + z + "]</dark_gray> <gray>" + (det != null ? det : "") + "</gray>");
                            }
                        }

                        if (count == 0) {
                            msg(sender, "<gray>No action logs found for " + target + " before " + finalDisplayTime + ".</gray>");
                        } else if (count > 8) {
                            msg(sender, "<gray>... and " + (count - 8) + " more actions (opened in GUI).</gray>");
                        }

                        if (sender instanceof Player player) {
                            Bukkit.getScheduler().runTask(plugin, () -> new LogsListGui(plugin, interactiveGui, LogsInteractiveGui.SubsystemCategory.BLOCK_ACTIONS, target, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Error querying action logs: " + e.getMessage() + "</red>");
            }
        });
    }

    private void handleSessions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs sessions <player> [date] [time]</red>");
            return;
        }
        String targetName = args[1];
        Instant targetInstant = null;
        String displayTime = "Latest";
        if (args.length >= 4) {
            targetInstant = parseDateTime(args[2] + "_" + args[3]);
            if (targetInstant == null) targetInstant = parseDateTime(args[2] + " " + args[3]);
            displayTime = args[2] + " " + args[3];
        } else if (args.length == 3) {
            targetInstant = parseRelativeOrAbsoluteTime(args[2]);
            displayTime = args[2];
        }
        handleSessionsDirect(sender, targetName, targetInstant != null ? targetInstant : Instant.now(), displayTime);
    }

    public void handleSessionsDirect(CommandSender sender, String target) {
        handleSessionsDirect(sender, target, Instant.now(), "Latest");
    }

    public void handleSessionsDirect(CommandSender sender, String target, Instant queryTime, String finalDisplayTime) {
        msg(sender, "<gray>Querying session history for <white>" + target + "</white> before <yellow>" + finalDisplayTime + "</yellow>...</gray>");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                boolean isLatest = "Latest".equalsIgnoreCase(finalDisplayTime);
                String sql = isLatest
                        ? "SELECT session_id, platform, joined_at, quit_at, duration_seconds, quit_reason FROM player_sessions WHERE username = ? ORDER BY joined_at DESC LIMIT 28"
                        : "SELECT session_id, platform, joined_at, quit_at, duration_seconds, quit_reason FROM player_sessions WHERE username = ? AND joined_at <= ? ORDER BY joined_at DESC LIMIT 28";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    if (!isLatest) ps.setTimestamp(2, Timestamp.from(queryTime));

                    try (ResultSet rs = ps.executeQuery()) {
                        List<LogsListGui.LogDisplayEntry> entries = new ArrayList<>();
                        msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Session History: " + target + " (" + finalDisplayTime + ") ===</bold></gradient>");
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            String sessionId = rs.getString("session_id");
                            String platform = rs.getString("platform");
                            String joined = rs.getString("joined_at");
                            String quit = rs.getString("quit_at");
                            int duration = rs.getInt("duration_seconds");
                            String reason = rs.getString("quit_reason");

                            List<String> details = new ArrayList<>(List.of(
                                    "<gray>Platform: <aqua>" + platform + "</aqua></gray>",
                                    "<gray>Joined: <yellow>" + joined + "</yellow></gray>",
                                    "<gray>Quit: <yellow>" + (quit != null ? quit : "Active / Online") + "</yellow></gray>",
                                    "<gray>Duration: <gold>" + duration / 60 + "m " + duration % 60 + "s</gold></gray>"
                            ));
                            if (reason != null && !reason.isBlank()) details.add("<gray>Quit Reason: <white>" + reason + "</white></gray>");

                            String label = "<gradient:#e879f9:#c084fc>Session</gradient> <yellow>" + (joined != null ? joined.substring(0, Math.min(10, joined.length())) : sessionId) + "</yellow>";
                            entries.add(new LogsListGui.LogDisplayEntry(Material.CLOCK, label, details));
                            if (count <= 8) {
                                msg(sender, "<gray>Joined: <white>" + joined + "</white> | Quit: <white>" + (quit != null ? quit : "Active/Online") + "</white> | Duration: <yellow>" + duration / 60 + "m</yellow> | Platform: <aqua>" + platform + "</aqua></gray>");
                            }
                        }

                        if (count == 0) {
                            msg(sender, "<gray>No recorded sessions found for " + target + " before " + finalDisplayTime + ".</gray>");
                        } else if (count > 8) {
                            msg(sender, "<gray>... and " + (count - 8) + " more sessions (opened in GUI).</gray>");
                        }

                        if (sender instanceof Player player) {
                            Bukkit.getScheduler().runTask(plugin, () -> new LogsListGui(plugin, interactiveGui, LogsInteractiveGui.SubsystemCategory.SESSIONS, target, finalDisplayTime, entries, 0).open(player));
                        }
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Error querying sessions: " + e.getMessage() + "</red>");
            }
        });
    }

    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs search <item-material></red>");
            return;
        }
        String query = "%" + args[1].toUpperCase(Locale.ROOT) + "%";
        msg(sender, "<gray>Searching item logs for <white>" + args[1] + "</white>...</gray>");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                String sql = "SELECT event_type, actor_name, material, amount, world_name, x, y, z, timestamp FROM player_item_events WHERE material LIKE ? OR item_display_name LIKE ? ORDER BY timestamp DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, query);
                    ps.setString(2, query);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean found = false;
                        msg(sender, "<gradient:#6366f1:#a855f7><bold>=== Search Results: " + args[1] + " ===</bold></gradient>");
                        while (rs.next()) {
                            found = true;
                            msg(sender, "<gray>" + rs.getString("timestamp") + "</gray> <white>" + rs.getString("actor_name") + "</white> <yellow>" + rs.getString("event_type") + "</yellow> <gold>" + rs.getInt("amount") + "x " + rs.getString("material") + "</gold>");
                        }
                        if (!found) msg(sender, "<gray>No matching item events found.</gray>");
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>Search failed: " + e.getMessage() + "</red>");
            }
        });
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "<gradient:#93c5fd:#3b82f6><bold>Moonlight Core • Audit & Log Engine</bold></gradient>");
        msg(sender, "<yellow>/logs <player> <date> <time></yellow> <gray>- Inspect inventory at DD/MM/YYYY HH:MM:SS</gray>");
        msg(sender, "<yellow>/logs echest <player> [date] [time]</yellow> <gray>- Inspect Ender Chest log</gray>");
        msg(sender, "<yellow>/logs position <player> [date] [time]</yellow> <gray>- Query coordinate trail & location</gray>");
        msg(sender, "<yellow>/logs snapshot <player></yellow> <gray>- Take immediate live inventory snapshot</gray>");
        msg(sender, "<yellow>/logs items <player></yellow> <gray>- Query pickup/drop/death item audit logs</gray>");
        msg(sender, "<yellow>/logs actions <player></yellow> <gray>- Query block & container action history</gray>");
        msg(sender, "<yellow>/logs sessions <player></yellow> <gray>- View session duration & platform history</gray>");
        msg(sender, "<yellow>/logs search <material></yellow> <gray>- Search audit trail for specific item</gray>");
        msg(sender, "<yellow>/logs export <player></yellow> <gray>- Export sanitized CSV report</gray>");
    }

    private void handleSnapshot(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs snapshot <player></red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            msg(sender, "<red>Player not found or offline.</red>");
            return;
        }
        String snapId = plugin.getAuditManager().getDatabaseManager().recordInventorySnapshot(target, "manual", UUID.randomUUID().toString());
        msg(sender, "<green>Created manual snapshot for <white>" + target.getName() + "</white>: <yellow>" + snapId + "</yellow></green>");
    }

    private void handleInventorySub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs inventory <player> [date] [time]</red> <gray>(Alias: /logs inv <player> ...)</gray>");
            return;
        }
        String targetName = args[1];
        if (args.length >= 4) {
            Instant instant = parseDateTime(args[2] + "_" + args[3]);
            queryAndOpenSnapshot(sender, targetName, instant != null ? instant : Instant.now(), args[2] + " " + args[3]);
        } else if (args.length == 3) {
            Instant instant = parseDateTime(args[2]);
            queryAndOpenSnapshot(sender, targetName, instant != null ? instant : Instant.now(), args[2]);
        } else {
            queryAndOpenSnapshot(sender, targetName, Instant.now(), "Latest");
        }
    }

    private void handleExport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs export <player></red>");
            return;
        }
        String player = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File dir = new File(plugin.getDataFolder(), "exports");
                if (!dir.exists()) dir.mkdirs();
                File exportFile = new File(dir, "audit_" + player + "_" + System.currentTimeMillis() + ".csv");
                try (FileWriter writer = new FileWriter(exportFile)) {
                    writer.write("Timestamp,Type,Player,Details\n");
                    writer.write(Instant.now() + ",EXPORT_SUMMARY," + sanitizeCsvField(player) + "," + sanitizeCsvField("Audit export generated for " + player) + "\n");
                }
                msg(sender, "<green>Export generated safely at: <yellow>plugins/MoonlightVanillaSurvivalCore/exports/" + exportFile.getName() + "</yellow></green>");
            } catch (Exception e) {
                msg(sender, "<red>Export failed: " + e.getMessage() + "</red>");
            }
        });
    }

    private void handlePrivacy(CommandSender sender, String[] args) {
        msg(sender, "<green>Data Privacy Summary: Player IP tracking is disabled by default. Sensitive logs are strictly OP-only.</green>");
    }

    private void handleIpLookup(CommandSender sender, String[] args) {
        if (!plugin.getAuditManager().getConfig().isTrackIpAddresses()) {
            msg(sender, "<red>IP tracking is disabled in audit.yml (logging.track-ip-addresses: false).</red>");
            return;
        }
        if (args.length < 2) {
            msg(sender, "<red>Usage: /logs ip <player></red>");
            return;
        }
        String target = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getAuditManager().getDatabaseManager().getConnection()) {
                String sql = "SELECT DISTINCT ip_address, joined_at FROM player_sessions WHERE username = ? AND ip_address IS NOT NULL ORDER BY joined_at DESC LIMIT 5";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, target);
                    try (ResultSet rs = ps.executeQuery()) {
                        msg(sender, "<gradient:#6366f1:#a855f7><bold>=== IP History: " + target + " ===</bold></gradient>");
                        boolean found = false;
                        while (rs.next()) {
                            found = true;
                            msg(sender, "<gray>IP: <yellow>" + rs.getString("ip_address") + "</yellow> (seen at " + rs.getString("joined_at") + ")</gray>");
                        }
                        if (!found) msg(sender, "<gray>No IP records logged for " + target + ".</gray>");
                    }
                }
            } catch (Exception e) {
                msg(sender, "<red>IP query error: " + e.getMessage() + "</red>");
            }
        });
    }

    public static String sanitizeCsvField(String field) {
        if (field == null) return "";
        if (field.startsWith("=") || field.startsWith("+") || field.startsWith("-") || field.startsWith("@")) {
            field = "'" + field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    public Instant parseDateTime(String input) {
        if (input == null || input.isBlank()) return null;
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try { return formatter.parse(input, Instant::from); } catch (Exception ignored) {}
        }
        return null;
    }

    public Instant parseRelativeOrAbsoluteTime(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) return null;
        String input = rawInput.trim();
        String lower = input.toLowerCase(Locale.ROOT);

        if (Set.of("now", "latest", "present").contains(lower)) return Instant.now();
        if (lower.equals("yesterday")) return Instant.now().minus(1, ChronoUnit.DAYS);
        if (lower.equals("today")) return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

        Pattern relPattern = Pattern.compile("(?i)(\\d+)\\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)");
        Matcher matcher = relPattern.matcher(input);
        boolean matchedAny = false;
        long totalSeconds = 0;
        int lastEnd = 0;

        while (matcher.find()) {
            matchedAny = true;
            try {
                long val = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toLowerCase(Locale.ROOT);
                if (unit.startsWith("s")) totalSeconds += val;
                else if (unit.startsWith("m")) totalSeconds += val * 60;
                else if (unit.startsWith("h")) totalSeconds += val * 3600;
                else if (unit.startsWith("d")) totalSeconds += val * 86400;
                else if (unit.startsWith("w")) totalSeconds += val * 604800;
            } catch (NumberFormatException ignored) {}
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
                return inst.isAfter(Instant.now()) ? time.atDate(today.minusDays(1)).atZone(ZoneId.systemDefault()).toInstant() : inst;
            } catch (Exception ignored) {}
        }

        Instant parsed = parseDateTime(input);
        if (parsed != null) return parsed;
        parsed = parseDateTime(input + "_12:00:00");
        return parsed != null ? parsed : parseDateTime(input.replace(" ", "_"));
    }

    private static List<String> filterPrefix(Collection<String> pool, String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        return pool.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).distinct().toList();
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!isAuthorized(sender)) return List.of();

        List<String> visiblePlayers = Bukkit.getOnlinePlayers().stream().filter(p -> VanishUtil.canSee(sender, p)).map(Player::getName).toList();
        List<String> subCommands = List.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv", "snapshot", "items", "actions", "sessions", "search", "export", "privacy", "ip", "help");
        List<String> timePresets = List.of("5m", "10m", "15m", "30m", "1h", "6h", "12h", "1d", "7d", "now", "yesterday");
        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        if (args.length <= 1) {
            String q = args.length == 0 ? "" : args[0];
            List<String> list = new ArrayList<>(filterPrefix(visiblePlayers, q));
            list.addAll(filterPrefix(subCommands, q));
            return list;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            String q = args[1];
            if (Set.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv", "snapshot", "items", "actions", "sessions", "export", "privacy", "ip").contains(first)) {
                return filterPrefix(visiblePlayers, q);
            }
            List<String> list = new ArrayList<>(filterPrefix(timePresets, q));
            if (todayDate.toLowerCase(Locale.ROOT).startsWith(q.toLowerCase(Locale.ROOT))) list.add(todayDate);
            if ("dd/mm/yyyy".startsWith(q.toLowerCase(Locale.ROOT))) list.add("DD/MM/YYYY");
            return list;
        }

        if (args.length == 3) {
            String q = args[2];
            if (Set.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv").contains(first)) {
                List<String> list = new ArrayList<>(filterPrefix(timePresets, q));
                if (todayDate.toLowerCase(Locale.ROOT).startsWith(q.toLowerCase(Locale.ROOT))) list.add(todayDate);
                if ("dd/mm/yyyy".startsWith(q.toLowerCase(Locale.ROOT))) list.add("DD/MM/YYYY");
                return list;
            }
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            List<String> timeList = List.of(nowTime, "HH:MM:SS", "12:00:00", "18:00:00", "00:00:00");
            return filterPrefix(timeList, q);
        }

        if (args.length == 4 && Set.of("echest", "ec", "enderchest", "position", "pos", "inventory", "inv").contains(first)) {
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            return filterPrefix(List.of(nowTime, "HH:MM:SS", "12:00:00", "18:00:00"), args[3]);
        }

        return List.of();
    }
}
