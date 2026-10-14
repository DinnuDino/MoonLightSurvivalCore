/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.audit.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MoonlightAuditLogEvent
extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String eventType;
    private final String playerUuid;
    private final String details;

    public MoonlightAuditLogEvent(String eventType, String playerUuid, String details) {
        super(true);
        this.eventType = eventType;
        this.playerUuid = playerUuid;
        this.details = details;
    }

    public String getEventType() {
        return this.eventType;
    }

    public String getPlayerUuid() {
        return this.playerUuid;
    }

    public String getDetails() {
        return this.details;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

