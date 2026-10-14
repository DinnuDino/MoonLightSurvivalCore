-- ==============================================================================
-- Moonlight Core Audit, Inventory Snapshot & Safe Refund Database Schema
-- Engines: MySQL 8.0+ / MariaDB 10.6+ / SQLite 3
-- All timestamps stored in UTC. Indexed for high-speed lookups and async batching.
-- ==============================================================================

-- 1. Player Session Events (Joins, Quits, Durations, Platform)
CREATE TABLE IF NOT EXISTS player_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    player_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(32) NOT NULL,
    platform VARCHAR(16) NOT NULL DEFAULT 'JAVA',
    ip_address VARCHAR(64) NULL,
    joined_at TIMESTAMP NOT NULL,
    quit_at TIMESTAMP NULL,
    duration_seconds BIGINT DEFAULT 0,
    quit_reason VARCHAR(64) NULL,
    INDEX idx_sessions_player_time (player_uuid, joined_at)
);

-- 2. Player Position Snapshots (Periodical, Teleports, Deaths)
CREATE TABLE IF NOT EXISTS player_position_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(32) NOT NULL,
    world_uuid VARCHAR(36) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    yaw FLOAT NOT NULL,
    pitch FLOAT NOT NULL,
    gamemode VARCHAR(16) NOT NULL,
    health DOUBLE NOT NULL,
    food_level INT NOT NULL,
    exp_level INT NOT NULL,
    snapshot_reason VARCHAR(32) NOT NULL,
    duration_seconds INT DEFAULT 1,
    last_active_timestamp TIMESTAMP NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_pos_player_time (player_uuid, timestamp)
);

-- 3. Player Inventory Snapshots (Full ItemStacks, Armor, Offhand, Ender Chest, XP)
CREATE TABLE IF NOT EXISTS player_inventory_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id VARCHAR(36) NOT NULL UNIQUE,
    player_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(32) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    gamemode VARCHAR(16) NOT NULL,
    health DOUBLE NOT NULL,
    hunger INT NOT NULL,
    saturation FLOAT NOT NULL,
    exp_level INT NOT NULL,
    total_exp INT NOT NULL,
    selected_slot INT NOT NULL,
    snapshot_reason VARCHAR(32) NOT NULL,
    inventory_data MEDIUMTEXT NOT NULL,
    armor_data TEXT NOT NULL,
    offhand_data TEXT NOT NULL,
    ender_chest_data MEDIUMTEXT NOT NULL,
    cursor_data TEXT NULL,
    potion_effects TEXT NULL,
    correlation_id VARCHAR(36) NULL,
    duration_seconds INT DEFAULT 1,
    last_active_timestamp TIMESTAMP NULL,
    state_hash VARCHAR(64) NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_inv_player_time (player_uuid, timestamp),
    INDEX idx_inv_correlation (correlation_id),
    INDEX idx_inv_hash (player_uuid, state_hash)
);

-- 4. Dedicated EnderChest Events (Opens, Closes, Item In/Out, Safe Logs)
CREATE TABLE IF NOT EXISTS player_enderchest_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(32) NOT NULL,
    material VARCHAR(64) NOT NULL,
    amount INT NOT NULL,
    item_display_name VARCHAR(128) NULL,
    item_serialized TEXT NULL,
    world_name VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    details TEXT NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_echest_player_time (player_uuid, timestamp),
    INDEX idx_echest_event_time (event_type, timestamp)
);

-- 4. Player Item Events (Pickup, Drop, Craft, Smelt, Enchant, Container, Death)
CREATE TABLE IF NOT EXISTS player_item_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    actor_uuid VARCHAR(36) NOT NULL,
    actor_name VARCHAR(32) NOT NULL,
    affected_player_uuid VARCHAR(36) NOT NULL,
    affected_player_name VARCHAR(32) NOT NULL,
    material VARCHAR(64) NOT NULL,
    amount INT NOT NULL,
    item_display_name VARCHAR(128) NULL,
    item_serialized TEXT NULL,
    world_name VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    details TEXT NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_item_affected_time (affected_player_uuid, timestamp),
    INDEX idx_item_actor_time (actor_uuid, timestamp),
    INDEX idx_item_event_time (event_type, timestamp),
    INDEX idx_item_correlation (correlation_id)
);

-- 5. Player Action Logs (Blocks, Containers, Deaths, Commands, Chat)
CREATE TABLE IF NOT EXISTS player_action_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(32) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    target_info VARCHAR(128) NULL,
    details TEXT NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_action_player_time (player_uuid, timestamp),
    INDEX idx_action_event_time (event_type, timestamp)
);

-- 6. OP Admin Actions (Strict Audit Trail of every Staff/OP command)
CREATE TABLE IF NOT EXISTS audit_admin_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(32) NOT NULL,
    operator_uuid VARCHAR(36) NOT NULL,
    operator_name VARCHAR(32) NOT NULL,
    target_uuid VARCHAR(36) NULL,
    target_name VARCHAR(32) NULL,
    reason TEXT NOT NULL,
    source_id VARCHAR(64) NULL,
    items_affected TEXT NULL,
    result VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    INDEX idx_admin_op_time (operator_uuid, timestamp),
    INDEX idx_admin_target_time (target_uuid, timestamp),
    INDEX idx_admin_correlation (correlation_id)
);

-- 7. Refund Transactions (Every Item Refund with Undo capability)
CREATE TABLE IF NOT EXISTS audit_refund_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL UNIQUE,
    operator_uuid VARCHAR(36) NOT NULL,
    operator_name VARCHAR(32) NOT NULL,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    refund_type VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    items_json MEDIUMTEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    before_snapshot_id VARCHAR(36) NOT NULL,
    after_snapshot_id VARCHAR(36) NULL,
    undone_by_op VARCHAR(36) NULL,
    undone_at TIMESTAMP NULL,
    undo_reason TEXT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_refund_target_time (target_uuid, created_at),
    INDEX idx_refund_op_time (operator_uuid, created_at),
    INDEX idx_refund_transaction (transaction_id),
    INDEX idx_refund_correlation (correlation_id)
);

-- 8. Restore Transactions (Two-step Confirmed Inventory Restores)
CREATE TABLE IF NOT EXISTS audit_restore_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restore_id VARCHAR(36) NOT NULL UNIQUE,
    operator_uuid VARCHAR(36) NOT NULL,
    operator_name VARCHAR(32) NOT NULL,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    source_snapshot_id VARCHAR(36) NOT NULL,
    confirmation_token VARCHAR(36) NOT NULL,
    reason TEXT NOT NULL,
    items_diff_json MEDIUMTEXT NOT NULL,
    before_snapshot_id VARCHAR(36) NOT NULL,
    after_snapshot_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_restore_target_time (target_uuid, created_at),
    INDEX idx_restore_correlation (correlation_id)
);

-- 9. Player Refund Mailbox (Holds items when target inventory is full)
CREATE TABLE IF NOT EXISTS player_refund_mailbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mailbox_item_id VARCHAR(36) NOT NULL UNIQUE,
    player_uuid VARCHAR(36) NOT NULL,
    source_transaction_id VARCHAR(36) NOT NULL,
    material VARCHAR(64) NOT NULL,
    amount INT NOT NULL,
    item_serialized MEDIUMTEXT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    granted_by VARCHAR(36) NOT NULL,
    claimed BOOLEAN DEFAULT FALSE,
    claimed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_mailbox_player (player_uuid, claimed)
);

-- 10. Player Timed Bans & Punishments (Unique Ban ID, Dynamic Expiry, Audit Trail)
CREATE TABLE IF NOT EXISTS player_bans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ban_id VARCHAR(16) NOT NULL UNIQUE,
    target_uuid VARCHAR(36) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    banned_by_uuid VARCHAR(36) NOT NULL,
    banned_by_name VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    duration_string VARCHAR(32) NOT NULL DEFAULT 'permanent',
    is_active BOOLEAN DEFAULT TRUE,
    unbanned_at TIMESTAMP NULL,
    unbanned_by VARCHAR(32) NULL,
    unban_reason VARCHAR(255) NULL,
    ip_address VARCHAR(64) NULL,
    correlation_id VARCHAR(36) NOT NULL,
    INDEX idx_bans_target_active (target_name, is_active),
    INDEX idx_bans_uuid_active (target_uuid, is_active),
    INDEX idx_bans_id (ban_id),
    INDEX idx_bans_correlation (correlation_id)
);
