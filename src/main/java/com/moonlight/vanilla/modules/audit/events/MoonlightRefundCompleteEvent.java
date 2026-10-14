/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla.modules.audit.events;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MoonlightRefundCompleteEvent
extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String transactionId;
    private final String opName;
    private final String targetName;
    private final List<ItemStack> items;
    private final String reason;

    public MoonlightRefundCompleteEvent(String transactionId, String opName, String targetName, List<ItemStack> items, String reason) {
        this.transactionId = transactionId;
        this.opName = opName;
        this.targetName = targetName;
        this.items = items;
        this.reason = reason;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getOpName() {
        return this.opName;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public String getReason() {
        return this.reason;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

