/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.moonlight.vanilla.modules.punishments;

import com.moonlight.vanilla.MoonlightVanillaSurvivalCore;
import com.moonlight.vanilla.modules.punishments.BanRecord;
import com.moonlight.vanilla.utils.MessageUtil;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BanManager {
    private final MoonlightVanillaSurvivalCore plugin;
    private final Map<UUID, BanRecord> activeBansByUuid = new ConcurrentHashMap<UUID, BanRecord>();
    private final Map<String, BanRecord> activeBansByName = new ConcurrentHashMap<String, BanRecord>();
    private final Map<String, BanRecord> activeBansById = new ConcurrentHashMap<String, BanRecord>();
    private static final String BAN_ID_CHARS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final Pattern DURATION_TOKEN_PATTERN = Pattern.compile("^(?:\\d+\\s*(?:s|sec|secs|seconds?|m|min|mins|minutes?|h|hr|hrs|hours?|d|day|days?|w|wk|weeks?|mo|mon|months?|y|yr|years?))+$", 2);
    public static final Pattern COMPONENT_PATTERN = Pattern.compile("(\\d+)\\s*(s|sec|secs|seconds?|m|min|mins|minutes?|h|hr|hrs|hours?|d|day|days?|w|wk|weeks?|mo|mon|months?|y|yr|years?)", 2);

    public BanManager(MoonlightVanillaSurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.createTableIfNotExists();
        this.loadActiveBansAsync();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS player_bans (id INTEGER PRIMARY KEY AUTOINCREMENT, ban_id VARCHAR(32) UNIQUE NOT NULL, target_uuid VARCHAR(36) NOT NULL, target_name VARCHAR(16) NOT NULL, banned_by_uuid VARCHAR(36) NOT NULL, banned_by_name VARCHAR(32) NOT NULL, reason TEXT NOT NULL, issued_at TIMESTAMP NOT NULL, expires_at TIMESTAMP, duration_string VARCHAR(32) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT 1, unbanned_at TIMESTAMP, unbanned_by VARCHAR(32), unban_reason TEXT, ip_address VARCHAR(45), correlation_id VARCHAR(64));";
        try (Connection conn = this.plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();){
            stmt.execute(sql);
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to create player_bans table!", e);
        }
    }

    public String generateBanId() {
        StringBuilder sb = new StringBuilder("#MNL-");
        for (int i = 0; i < 5; ++i) {
            sb.append(BAN_ID_CHARS.charAt(RANDOM.nextInt(BAN_ID_CHARS.length())));
        }
        return sb.toString();
    }

    public boolean isDurationToken(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return DURATION_TOKEN_PATTERN.matcher(trimmed).matches();
    }

    public static String formatDuration(long millis) {
        long totalSecs = Math.max(0L, millis / 1000L);
        long days = totalSecs / 86400L;
        long hours = totalSecs % 86400L / 3600L;
        long minutes = totalSecs % 3600L / 60L;
        long seconds = totalSecs % 60L;
        StringBuilder sb = new StringBuilder();
        if (days > 0L) {
            sb.append(days).append("d ");
        }
        sb.append(hours).append("h ").append(minutes).append("m ").append(seconds).append("s");
        return sb.toString();
    }

    public Long parseDuration(String input) throws IllegalArgumentException {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String clean = input.trim();
        if (clean.equalsIgnoreCase("perm") || clean.equalsIgnoreCase("permanent") || clean.equalsIgnoreCase("forever") || clean.equalsIgnoreCase("inf")) {
            return null;
        }
        Matcher matcher = COMPONENT_PATTERN.matcher(clean);
        long totalMillis = 0L;
        int lastEnd = 0;
        boolean foundAny = false;
        while (matcher.find()) {
            String gap = clean.substring(lastEnd, matcher.start()).trim();
            if (!gap.isEmpty()) {
                throw new IllegalArgumentException("Invalid duration component: " + gap);
            }
            long val = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            long multiplier = unit.startsWith("s") ? 1000L : (unit.startsWith("mo") ? 2592000000L : (unit.startsWith("m") ? 60000L : (unit.startsWith("h") ? 3600000L : (unit.startsWith("d") ? 86400000L : (unit.startsWith("w") ? 604800000L : (unit.startsWith("y") ? 31536000000L : 60000L))))));
            totalMillis += val * multiplier;
            lastEnd = matcher.end();
            foundAny = true;
        }
        if (!foundAny || !clean.substring(lastEnd).trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid duration format: " + input + " (Examples: 0h 0m 1s, 30m, 2hr, 1d, perm)");
        }
        if (totalMillis <= 0L) {
            throw new IllegalArgumentException("Duration must be at least 1 second.");
        }
        return totalMillis;
    }

    public BanRecord issueBan(String targetName, UUID targetUuid, String bannerName, UUID bannerUuid, Long durationMillis, String durationString, String reason, String ipAddress) {
        String banId = this.generateBanId();
        long now = System.currentTimeMillis();
        Long expiresAt = durationMillis != null ? Long.valueOf(now + durationMillis) : null;
        String correlationId = "ban-" + UUID.randomUUID().toString().substring(0, 8);
        String cleanDuration = durationMillis != null ? BanManager.formatDuration(durationMillis) : "permanent";
        BanRecord record = new BanRecord(0L, banId, targetUuid, targetName, bannerUuid, bannerName, reason, now, expiresAt, cleanDuration, true, null, null, null, ipAddress, correlationId);
        this.activeBansByUuid.put(targetUuid, record);
        this.activeBansByName.put(targetName.toLowerCase(), record);
        this.activeBansById.put(banId.toUpperCase(), record);
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            this.saveBanToDatabase(record);
            this.plugin.getAuditManager().logAdminAction(bannerUuid, bannerName, targetUuid != null ? targetUuid.toString() : null, targetName, "BAN_ISSUED", reason, banId, "Duration: " + durationString + " | Expires: " + String.valueOf(expiresAt == null ? "Never" : expiresAt), "SUCCESS", correlationId);
        });
        Player targetPlayer = Bukkit.getPlayer((UUID)targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                String kickMsg = this.buildKickScreen(record);
                targetPlayer.kick(MessageUtil.format(kickMsg));
            });
        }
        return record;
    }

    public boolean unban(String targetOrBanId, String unbannerName, String unbanReason) {
        BanRecord record = null;
        if (targetOrBanId.startsWith("#") || this.activeBansById.containsKey(targetOrBanId.toUpperCase())) {
            record = this.activeBansById.get(targetOrBanId.toUpperCase());
        }
        if (record == null) {
            record = this.activeBansByName.get(targetOrBanId.toLowerCase());
        }
        if (record == null || !record.isActive()) {
            return false;
        }
        record.setActive(false);
        record.setUnbannedAt(System.currentTimeMillis());
        record.setUnbannedBy(unbannerName);
        record.setUnbanReason(unbanReason);
        this.activeBansByUuid.remove(record.getTargetUuid());
        this.activeBansByName.remove(record.getTargetName().toLowerCase());
        this.activeBansById.remove(record.getBanId().toUpperCase());
        BanRecord finalRecord = record;
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            this.updateBanInDatabase(finalRecord);
            this.plugin.getAuditManager().logAdminAction(UUID.randomUUID(), unbannerName, finalRecord.getTargetUuid() != null ? finalRecord.getTargetUuid().toString() : null, finalRecord.getTargetName(), "UNBAN_EXECUTED", unbanReason, finalRecord.getBanId(), "Pardoned ban " + finalRecord.getBanId(), "SUCCESS", "unban-" + UUID.randomUUID().toString().substring(0, 8));
        });
        return true;
    }

    public String buildKickScreen(BanRecord ban) {
        ZoneId zone = ZoneId.of("UTC");
        String liveRemaining = ban.getFormattedRemaining();
        String expiryDate = ban.getFormattedExpiryDate(zone);
        return "<red><bold>MOONLIGHT SURVIVAL</bold></red> <dark_gray>-</dark_gray> <dark_red><bold>PUNISHMENT SYSTEM</bold></dark_red>\n\n<gray>You are suspended from Moonlight Vanilla Survival!</gray>\n\n<white>Reason:</white> <yellow>" + ban.getReason() + "</yellow>\n<white>Banned By:</white> <gold>" + ban.getBannedByName() + "</gold>\n<white>Time Remaining:</white> <aqua><bold>" + liveRemaining + "</bold></aqua> <gray>(Live Dynamic)</gray>\n<white>Ban ID:</white> <light_purple>" + ban.getBanId() + "</light_purple>\n<white>Expires On:</white> <gray>" + expiryDate + "</gray>\n\n<dark_gray>\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500</dark_gray>\n<gray>If you believe this was an error, appeal on Discord:</gray> <aqua>discord.gg/BVEa5UbCHg</aqua>\n<gray>Include your unique Ban ID: </gray><light_purple>" + ban.getBanId() + "</light_purple>";
    }

    public BanRecord getActiveBan(UUID uuid) {
        BanRecord record = this.activeBansByUuid.get(uuid);
        if (record != null && record.isExpired()) {
            this.expireBan(record);
            return null;
        }
        return record;
    }

    public BanRecord getActiveBanByName(String name) {
        BanRecord record = this.activeBansByName.get(name.toLowerCase());
        if (record != null && record.isExpired()) {
            this.expireBan(record);
            return null;
        }
        return record;
    }

    public BanRecord getBanById(String banId) {
        return this.activeBansById.get(banId.toUpperCase());
    }

    public void expireBan(BanRecord record) {
        record.setActive(false);
        this.activeBansByUuid.remove(record.getTargetUuid());
        this.activeBansByName.remove(record.getTargetName().toLowerCase());
        this.activeBansById.remove(record.getBanId().toUpperCase());
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            this.updateBanInDatabase(record);
            this.plugin.getAuditManager().logAdminAction(UUID.fromString("00000000-0000-0000-0000-000000000000"), "SYSTEM", record.getTargetUuid() != null ? record.getTargetUuid().toString() : null, record.getTargetName(), "BAN_EXPIRED", "Timed ban duration elapsed", record.getBanId(), "Ban expired automatically", "SUCCESS", "exp-" + UUID.randomUUID().toString().substring(0, 8));
        });
    }

    private void saveBanToDatabase(BanRecord ban) {
        String sql = "INSERT INTO player_bans (ban_id, target_uuid, target_name, banned_by_uuid, banned_by_name, reason, issued_at, expires_at, duration_string, is_active, ip_address, correlation_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = this.plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, 1);){
            ps.setString(1, ban.getBanId());
            ps.setString(2, ban.getTargetUuid().toString());
            ps.setString(3, ban.getTargetName());
            ps.setString(4, ban.getBannedByUuid().toString());
            ps.setString(5, ban.getBannedByName());
            ps.setString(6, ban.getReason());
            ps.setTimestamp(7, new Timestamp(ban.getIssuedAt()));
            ps.setTimestamp(8, ban.getExpiresAt() != null ? new Timestamp(ban.getExpiresAt()) : null);
            ps.setString(9, ban.getDurationString());
            ps.setBoolean(10, ban.isActive());
            ps.setString(11, ban.getIpAddress());
            ps.setString(12, ban.getCorrelationId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys();){
                if (rs.next()) {
                    ban.setId(rs.getLong(1));
                }
            }
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save ban record to database!", e);
        }
    }

    private void updateBanInDatabase(BanRecord ban) {
        String sql = "UPDATE player_bans SET is_active = ?, unbanned_at = ?, unbanned_by = ?, unban_reason = ? WHERE ban_id = ?";
        try (Connection conn = this.plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);){
            ps.setBoolean(1, ban.isActive());
            ps.setTimestamp(2, ban.getUnbannedAt() != null ? new Timestamp(ban.getUnbannedAt()) : null);
            ps.setString(3, ban.getUnbannedBy());
            ps.setString(4, ban.getUnbanReason());
            ps.setString(5, ban.getBanId());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to update ban record in database!", e);
        }
    }

    private void loadActiveBansAsync() {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            String sql = "SELECT * FROM player_bans WHERE is_active = TRUE";
            try (Connection conn = this.plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery();){
                while (rs.next()) {
                    String correlationId;
                    String ip;
                    String durationStr;
                    Timestamp expTs;
                    Long expiresAt;
                    long issuedAt;
                    String reason;
                    String bannerName;
                    UUID bannerUuid;
                    String targetName;
                    UUID targetUuid;
                    String banId;
                    long id = rs.getLong("id");
                    BanRecord record = new BanRecord(id, banId = rs.getString("ban_id"), targetUuid = UUID.fromString(rs.getString("target_uuid")), targetName = rs.getString("target_name"), bannerUuid = UUID.fromString(rs.getString("banned_by_uuid")), bannerName = rs.getString("banned_by_name"), reason = rs.getString("reason"), issuedAt = rs.getTimestamp("issued_at").getTime(), expiresAt = (expTs = rs.getTimestamp("expires_at")) != null ? Long.valueOf(expTs.getTime()) : null, durationStr = rs.getString("duration_string"), true, null, null, null, ip = rs.getString("ip_address"), correlationId = rs.getString("correlation_id"));
                    if (record.isExpired()) {
                        this.expireBan(record);
                        continue;
                    }
                    this.activeBansByUuid.put(targetUuid, record);
                    this.activeBansByName.put(targetName.toLowerCase(), record);
                    this.activeBansById.put(banId.toUpperCase(), record);
                }
                this.plugin.getLogger().info("[Punishments] Loaded " + this.activeBansById.size() + " active bans into memory cache.");
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to load active bans from database!", e);
            }
        });
    }
}

