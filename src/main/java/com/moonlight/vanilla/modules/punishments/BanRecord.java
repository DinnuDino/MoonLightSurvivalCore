/*
 * Decompiled with CFR 0.152.
 */
package com.moonlight.vanilla.modules.punishments;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BanRecord {
    private long id;
    private final String banId;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID bannedByUuid;
    private final String bannedByName;
    private final String reason;
    private final long issuedAt;
    private final Long expiresAt;
    private final String durationString;
    private boolean active;
    private Long unbannedAt;
    private String unbannedBy;
    private String unbanReason;
    private final String ipAddress;
    private final String correlationId;

    public BanRecord(long id, String banId, UUID targetUuid, String targetName, UUID bannedByUuid, String bannedByName, String reason, long issuedAt, Long expiresAt, String durationString, boolean active, Long unbannedAt, String unbannedBy, String unbanReason, String ipAddress, String correlationId) {
        this.id = id;
        this.banId = banId;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.bannedByUuid = bannedByUuid;
        this.bannedByName = bannedByName;
        this.reason = reason;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.durationString = durationString;
        this.active = active;
        this.unbannedAt = unbannedAt;
        this.unbannedBy = unbannedBy;
        this.unbanReason = unbanReason;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId;
    }

    public boolean isPermanent() {
        return this.expiresAt == null;
    }

    public boolean isExpired() {
        if (this.isPermanent()) {
            return false;
        }
        return System.currentTimeMillis() >= this.expiresAt;
    }

    public long getRemainingMillis() {
        if (this.isPermanent()) {
            return -1L;
        }
        long diff = this.expiresAt - System.currentTimeMillis();
        return Math.max(0L, diff);
    }

    public String getFormattedRemaining() {
        if (this.isPermanent()) {
            return "Permanent";
        }
        long diff = this.getRemainingMillis();
        if (diff <= 0L) {
            return "0h 0m 0s (Expired)";
        }
        long totalSecs = (diff + 999L) / 1000L;
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

    public String getFormattedExpiryDate(ZoneId zone) {
        if (this.isPermanent()) {
            return "Never";
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z").withZone(zone);
        return dtf.format(Instant.ofEpochMilli(this.expiresAt));
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBanId() {
        return this.banId;
    }

    public UUID getTargetUuid() {
        return this.targetUuid;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public UUID getBannedByUuid() {
        return this.bannedByUuid;
    }

    public String getBannedByName() {
        return this.bannedByName;
    }

    public String getReason() {
        return this.reason;
    }

    public long getIssuedAt() {
        return this.issuedAt;
    }

    public Long getExpiresAt() {
        return this.expiresAt;
    }

    public String getDurationString() {
        return this.durationString;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getUnbannedAt() {
        return this.unbannedAt;
    }

    public void setUnbannedAt(Long unbannedAt) {
        this.unbannedAt = unbannedAt;
    }

    public String getUnbannedBy() {
        return this.unbannedBy;
    }

    public void setUnbannedBy(String unbannedBy) {
        this.unbannedBy = unbannedBy;
    }

    public String getUnbanReason() {
        return this.unbanReason;
    }

    public void setUnbanReason(String unbanReason) {
        this.unbanReason = unbanReason;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getCorrelationId() {
        return this.correlationId;
    }
}

