/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.command.brigadier.BasicCommand
 *  io.papermc.paper.command.brigadier.CommandSourceStack
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.moonlight.vanilla.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaperCommandAdapter
implements BasicCommand {
    private final String name;
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;
    private final String permission;
    private final Command dummyCommand;

    public PaperCommandAdapter(String name, final CommandExecutor executor, TabCompleter tabCompleter, String permission) {
        this.name = name;
        this.executor = executor;
        this.tabCompleter = tabCompleter;
        this.permission = permission;
        this.dummyCommand = new Command(name){

            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return executor != null && executor.onCommand(sender, (Command)this, commandLabel, args);
            }
        };
        if (permission != null) {
            this.dummyCommand.setPermission(permission);
        }
    }

    public PaperCommandAdapter(String name, CommandExecutor executor, String permission) {
        this(name, executor, executor instanceof TabCompleter ? (TabCompleter)executor : null, permission);
    }

    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (this.executor != null) {
            CommandSender sender = stack.getSender();
            this.executor.onCommand(sender, this.dummyCommand, this.name, args);
        }
    }

    @NotNull
    public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (this.tabCompleter != null) {
            String[] stringArray;
            if (args == null || args.length == 0) {
                String[] stringArray2 = new String[1];
                stringArray = stringArray2;
                stringArray2[0] = "";
            } else {
                stringArray = args;
            }
            String[] effectiveArgs = stringArray;
            List list = this.tabCompleter.onTabComplete(stack.getSender(), this.dummyCommand, this.name, effectiveArgs);
            if (list != null) {
                return list;
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    public String permission() {
        return this.permission;
    }
}

