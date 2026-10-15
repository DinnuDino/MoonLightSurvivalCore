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
        if (tabCompleter != null) {
            String[] effectiveArgs = (args == null || args.length == 0) ? new String[]{""} : args;
            List<String> list = tabCompleter.onTabComplete(stack.getSender(), dummyCommand, name, effectiveArgs);
            if (list != null) return list;
        }
        return List.of();
    }

    @Nullable
    public String permission() {
        return this.permission;
    }
}

