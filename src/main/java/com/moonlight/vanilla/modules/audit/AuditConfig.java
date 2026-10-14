/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package com.moonlight.vanilla.modules.audit;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

public class AuditConfig {
    private boolean enabled;
    private ZoneId timezone;
    private boolean requireVanillaOp;
    private boolean allowConsole;
    private String noPermissionMessage;
    private int positionSnapshotIntervalSeconds;
    private int inventorySnapshotIntervalSeconds;
    private boolean deduplicationEnabled;
    private boolean logEnderChestEvents;
    private boolean chatEnabled;
    private boolean trackIpAddresses;
    private boolean logContainerTransactions;
    private boolean logInventoryEvents;
    private int batchSize;
    private int flushIntervalSeconds;
    private boolean permanentArchival;
    private boolean deleteDuplicateIdleRecordsOnly;
    private int positionRetentionDays;
    private int inventoryRetentionDays;
    private int generalAuditRetentionDays;
    private int adminActionsRetentionDays;
    private int refundTransactionsRetentionDays;
    private boolean refundsEnabled;
    private boolean requireReason;
    private int minimumReasonLength;
    private int maximumReasonLength;
    private int confirmationExpirySeconds;
    private String defaultDeliveryMode;
    private boolean allowFullInventoryRestore;
    private int maxItemsPerRefund;
    private int maxRefundsPerOpPerHour;
    private int maxRefundsPerTargetPerHour;
    private Set<String> blockedMaterials;
    private boolean allowUnsafeItems;
    private boolean exportsEnabled;
    private String exportFormat;
    private String exportDirectory;
    private int maximumExportRecords;
    private boolean showIpInNormalOutput;
    private boolean maskIpInAuditOutput;

    public void load(FileConfiguration config) {
        this.enabled = config.getBoolean("audit.enabled", true);
        String tzStr = config.getString("audit.timezone", "Asia/Kolkata");
        try {
            this.timezone = ZoneId.of(tzStr);
        }
        catch (Exception e) {
            this.timezone = ZoneId.of("UTC");
        }
        this.requireVanillaOp = config.getBoolean("audit.access.require-vanilla-op", true);
        this.allowConsole = config.getBoolean("audit.access.allow-console", true);
        this.noPermissionMessage = config.getString("audit.access.no-permission-message", "<red>Access denied: You must be a server operator (OP) to use audit logging and refund tools.</red>");
        this.positionSnapshotIntervalSeconds = config.getInt("audit.logging.position-snapshot-interval-seconds", 1);
        this.inventorySnapshotIntervalSeconds = config.getInt("audit.logging.inventory-snapshot-interval-seconds", 1);
        this.deduplicationEnabled = config.getBoolean("audit.logging.deduplication-enabled", true);
        this.logEnderChestEvents = config.getBoolean("audit.logging.log-enderchest-events", true);
        this.chatEnabled = config.getBoolean("audit.logging.chat-enabled", false);
        this.trackIpAddresses = config.getBoolean("audit.logging.track-ip-addresses", false);
        this.logContainerTransactions = config.getBoolean("audit.logging.log-container-transactions", true);
        this.logInventoryEvents = config.getBoolean("audit.logging.log-inventory-events", true);
        this.batchSize = config.getInt("audit.logging.batch-size", 100);
        this.flushIntervalSeconds = config.getInt("audit.logging.flush-interval-seconds", 5);
        this.permanentArchival = config.getBoolean("audit.retention.permanent-archival", true);
        this.deleteDuplicateIdleRecordsOnly = config.getBoolean("audit.retention.delete-duplicate-idle-records-only", true);
        this.positionRetentionDays = config.getInt("audit.retention.position-snapshots-days", 0);
        this.inventoryRetentionDays = config.getInt("audit.retention.inventory-snapshots-days", 0);
        this.generalAuditRetentionDays = config.getInt("audit.retention.general-audit-days", 0);
        this.adminActionsRetentionDays = config.getInt("audit.retention.admin-actions-days", 0);
        this.refundTransactionsRetentionDays = config.getInt("audit.retention.refund-transactions-days", 0);
        this.refundsEnabled = config.getBoolean("audit.refunds.enabled", true);
        this.requireReason = config.getBoolean("audit.refunds.require-reason", true);
        this.minimumReasonLength = config.getInt("audit.refunds.minimum-reason-length", 5);
        this.maximumReasonLength = config.getInt("audit.refunds.maximum-reason-length", 255);
        this.confirmationExpirySeconds = config.getInt("audit.refunds.confirmation-expiry-seconds", 60);
        this.defaultDeliveryMode = config.getString("audit.refunds.default-mode", "mailbox-if-full");
        this.allowFullInventoryRestore = config.getBoolean("audit.refunds.allow-full-inventory-restore", false);
        this.maxItemsPerRefund = config.getInt("audit.refunds.max-items-per-refund", 2304);
        this.maxRefundsPerOpPerHour = config.getInt("audit.refunds.max-refunds-per-op-per-hour", 20);
        this.maxRefundsPerTargetPerHour = config.getInt("audit.refunds.max-refunds-per-target-per-hour", 10);
        List<String> blocked = config.getStringList("audit.refunds.blocked-materials");
        this.blockedMaterials = new HashSet<String>();
        for (String m : blocked) {
            this.blockedMaterials.add(m.toUpperCase());
        }
        this.allowUnsafeItems = config.getBoolean("audit.refunds.allow-unsafe-items", false);
        this.exportsEnabled = config.getBoolean("audit.exports.enabled", true);
        this.exportFormat = config.getString("audit.exports.format", "csv");
        this.exportDirectory = config.getString("audit.exports.export-directory", "exports");
        this.maximumExportRecords = config.getInt("audit.exports.maximum-records", 100000);
        this.showIpInNormalOutput = config.getBoolean("audit.privacy.show-ip-in-normal-output", false);
        this.maskIpInAuditOutput = config.getBoolean("audit.privacy.mask-ip-in-audit-output", true);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public ZoneId getTimezone() {
        return this.timezone;
    }

    public boolean isRequireVanillaOp() {
        return this.requireVanillaOp;
    }

    public boolean isAllowConsole() {
        return this.allowConsole;
    }

    public String getNoPermissionMessage() {
        return this.noPermissionMessage;
    }

    public int getPositionSnapshotIntervalSeconds() {
        return this.positionSnapshotIntervalSeconds;
    }

    public int getInventorySnapshotIntervalSeconds() {
        return this.inventorySnapshotIntervalSeconds;
    }

    public boolean isDeduplicationEnabled() {
        return this.deduplicationEnabled;
    }

    public boolean isLogEnderChestEvents() {
        return this.logEnderChestEvents;
    }

    public boolean isPermanentArchival() {
        return this.permanentArchival;
    }

    public boolean isDeleteDuplicateIdleRecordsOnly() {
        return this.deleteDuplicateIdleRecordsOnly;
    }

    public boolean isChatEnabled() {
        return this.chatEnabled;
    }

    public boolean isTrackIpAddresses() {
        return this.trackIpAddresses;
    }

    public boolean isLogContainerTransactions() {
        return this.logContainerTransactions;
    }

    public boolean isLogInventoryEvents() {
        return this.logInventoryEvents;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public int getFlushIntervalSeconds() {
        return this.flushIntervalSeconds;
    }

    public int getPositionRetentionDays() {
        return this.positionRetentionDays;
    }

    public int getInventoryRetentionDays() {
        return this.inventoryRetentionDays;
    }

    public int getGeneralAuditRetentionDays() {
        return this.generalAuditRetentionDays;
    }

    public int getAdminActionsRetentionDays() {
        return this.adminActionsRetentionDays;
    }

    public int getRefundTransactionsRetentionDays() {
        return this.refundTransactionsRetentionDays;
    }

    public boolean isRefundsEnabled() {
        return this.refundsEnabled;
    }

    public boolean isRequireReason() {
        return this.requireReason;
    }

    public int getMinimumReasonLength() {
        return this.minimumReasonLength;
    }

    public int getMaximumReasonLength() {
        return this.maximumReasonLength;
    }

    public int getConfirmationExpirySeconds() {
        return this.confirmationExpirySeconds;
    }

    public String getDefaultDeliveryMode() {
        return this.defaultDeliveryMode;
    }

    public boolean isAllowFullInventoryRestore() {
        return this.allowFullInventoryRestore;
    }

    public int getMaxItemsPerRefund() {
        return this.maxItemsPerRefund;
    }

    public int getMaxRefundsPerOpPerHour() {
        return this.maxRefundsPerOpPerHour;
    }

    public int getMaxRefundsPerTargetPerHour() {
        return this.maxRefundsPerTargetPerHour;
    }

    public Set<String> getBlockedMaterials() {
        return this.blockedMaterials;
    }

    public boolean isAllowUnsafeItems() {
        return this.allowUnsafeItems;
    }

    public boolean isExportsEnabled() {
        return this.exportsEnabled;
    }

    public String getExportFormat() {
        return this.exportFormat;
    }

    public String getExportDirectory() {
        return this.exportDirectory;
    }

    public int getMaximumExportRecords() {
        return this.maximumExportRecords;
    }

    public boolean isShowIpInNormalOutput() {
        return this.showIpInNormalOutput;
    }

    public boolean isMaskIpInAuditOutput() {
        return this.maskIpInAuditOutput;
    }
}

