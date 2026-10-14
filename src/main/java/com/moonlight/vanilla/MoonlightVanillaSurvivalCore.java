/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.PluginIdentifiableCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.NotNull
 */
package com.moonlight.vanilla;

import com.moonlight.vanilla.commands.BanCommand;
import com.moonlight.vanilla.commands.BanInfoCommand;
import com.moonlight.vanilla.commands.ChangeNameCommand;
import com.moonlight.vanilla.commands.EnderChestCommand;
import com.moonlight.vanilla.commands.HelpAndRulesOverrideListener;
import com.moonlight.vanilla.commands.HelpCommand;
import com.moonlight.vanilla.commands.InvseeCommand;
import com.moonlight.vanilla.commands.KickCommand;
import com.moonlight.vanilla.commands.LinkCommand;
import com.moonlight.vanilla.commands.MuteCommand;
import com.moonlight.vanilla.commands.RulesCommand;
import com.moonlight.vanilla.commands.StaffHelpCommand;
import com.moonlight.vanilla.commands.UnbanCommand;
import com.moonlight.vanilla.commands.UnmuteCommand;
import com.moonlight.vanilla.commands.YouTubeCommand;
import com.moonlight.vanilla.database.DatabaseManager;
import com.moonlight.vanilla.modules.audit.AuditManager;
import com.moonlight.vanilla.modules.audit.commands.LogsCommand;
import com.moonlight.vanilla.modules.chat.commands.MessageCommand;
import com.moonlight.vanilla.modules.chat.commands.ReplyCommand;
import com.moonlight.vanilla.modules.chat.listeners.ChatAndJoinListener;
import com.moonlight.vanilla.modules.crossplay.BedrockCommand;
import com.moonlight.vanilla.modules.crossplay.BedrockManager;
import com.moonlight.vanilla.modules.crossplay.ViaBackwardsManager;
import com.moonlight.vanilla.modules.crossplay.ViaInfoCommand;
import com.moonlight.vanilla.modules.features.FeaturesModule;
import com.moonlight.vanilla.modules.gui.GuiSecurityListener;
import com.moonlight.vanilla.modules.homes.HomeManager;
import com.moonlight.vanilla.modules.homes.commands.DelHomeCommand;
import com.moonlight.vanilla.modules.homes.commands.HomeCommand;
import com.moonlight.vanilla.modules.homes.commands.SetHomeCommand;
import com.moonlight.vanilla.modules.proxy.ProxyManager;
import com.moonlight.vanilla.modules.proxy.commands.HubCommand;
import com.moonlight.vanilla.modules.proxy.commands.ServerCommand;
import com.moonlight.vanilla.modules.punishments.BanLoginListener;
import com.moonlight.vanilla.modules.punishments.BanManager;
import com.moonlight.vanilla.modules.punishments.MuteManager;
import com.moonlight.vanilla.modules.ranks.RanksCommand;
import com.moonlight.vanilla.modules.ranks.RanksGui;
import com.moonlight.vanilla.modules.ranks.RanksListener;
import com.moonlight.vanilla.modules.ranks.RanksManager;
import com.moonlight.vanilla.modules.resourcepack.ResourcePackCommand;
import com.moonlight.vanilla.modules.resourcepack.ResourcePackManager;
import com.moonlight.vanilla.modules.scoreboard.ScoreboardManager;
import com.moonlight.vanilla.modules.scoreboard.commands.ScoreboardCommand;
import com.moonlight.vanilla.modules.security.PluginHiderListener;
import com.moonlight.vanilla.modules.teleport.TeleportManager;
import com.moonlight.vanilla.modules.teleport.listeners.TeleportListener;
import com.moonlight.vanilla.modules.vault.PlayerVaultCommand;
import com.moonlight.vanilla.modules.vault.PlayerVaultManager;
import com.moonlight.vanilla.modules.world.SpawnManager;
import com.moonlight.vanilla.modules.world.WorldLocationManager;
import com.moonlight.vanilla.modules.world.listeners.WorldLocationListener;
import com.moonlight.vanilla.modules.wraps.WrapCommand;
import com.moonlight.vanilla.modules.wraps.WrapGui;
import com.moonlight.vanilla.modules.wraps.WrapListener;
import com.moonlight.vanilla.modules.wraps.WrapManager;
import com.moonlight.vanilla.security.SecurityAuditor;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class MoonlightVanillaSurvivalCore
extends JavaPlugin {
    private static MoonlightVanillaSurvivalCore instance;
    private DatabaseManager databaseManager;
    private HomeManager homeManager;
    private TeleportManager teleportManager;
    private SecurityAuditor securityAuditor;
    private BedrockManager bedrockManager;
    private ViaBackwardsManager viaBackwardsManager;
    private AuditManager auditManager;
    private BanManager banManager;
    private MuteManager muteManager;
    private ScoreboardManager scoreboardManager;
    private ResourcePackManager resourcePackManager;
    private MessageCommand messageCommand;
    private ProxyManager proxyManager;
    private WrapManager wrapManager;
    private WrapGui wrapGui;
    private WorldLocationManager worldLocationManager;
    private SpawnManager spawnManager;
    private HelpCommand helpCommand;
    private RulesCommand rulesCommand;
    private LinkCommand linkCommand;
    private YouTubeCommand youTubeCommand;
    private RanksManager ranksManager;
    private RanksGui ranksGui;
    private RanksCommand ranksCommand;
    private InvseeCommand invseeCommand;
    private WrapCommand wrapCommand;
    private ScoreboardCommand scoreboardCommand;
    private ChangeNameCommand changeNameCommand;
    private HomeCommand homeCommand;
    private SetHomeCommand setHomeCommand;
    private DelHomeCommand delHomeCommand;
    private LogsCommand logsCommand;
    private ReplyCommand replyCommand;
    private PlayerVaultManager playerVaultManager;
    private PlayerVaultCommand playerVaultCommand;

    public void onEnable() {
        Plugin mv;
        Plugin lp;
        instance = this;
        long startTime = System.currentTimeMillis();
        this.getLogger().info("=================================================");
        this.getLogger().info("Starting MoonlightVanillaSurvivalCore (Paper 26.2)");
        this.getLogger().info("=================================================");
        this.saveDefaultConfig();
        try {
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.initialize();
            this.getLogger().info("[Database] HikariCP connection pool active and ready.");
        }
        catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize database pool. Disabling plugin to prevent data corruption!", e);
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.securityAuditor = new SecurityAuditor(this);
        this.securityAuditor.enforceStartupAudit();
        this.homeManager = new HomeManager(this);
        this.teleportManager = new TeleportManager(this);
        this.bedrockManager = new BedrockManager(this);
        this.bedrockManager.initialize();
        this.viaBackwardsManager = new ViaBackwardsManager(this);
        this.viaBackwardsManager.initialize();
        this.auditManager = new AuditManager(this);
        this.auditManager.initialize();
        this.banManager = new BanManager(this);
        this.banManager.initialize();
        this.muteManager = new MuteManager(this);
        this.muteManager.initialize();
        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreboardManager.initialize();
        this.resourcePackManager = new ResourcePackManager(this);
        this.resourcePackManager.initialize();
        this.proxyManager = new ProxyManager(this);
        this.proxyManager.initialize();
        this.wrapManager = new WrapManager(this);
        this.wrapManager.initialize();
        this.wrapGui = new WrapGui(this, this.wrapManager);
        this.worldLocationManager = new WorldLocationManager(this);
        this.spawnManager = new SpawnManager(this);
        this.ranksManager = new RanksManager(this);
        this.ranksManager.initialize();
        this.ranksGui = new RanksGui(this, this.ranksManager);
        this.ranksCommand = new RanksCommand(this, this.ranksManager, this.ranksGui);
        this.registerCommands();
        this.registerListeners();
        if (!new File(this.getDataFolder(), "moonlight_logo.png").exists()) {
            try {
                this.saveResource("moonlight_logo.png", false);
            }
            catch (Exception e) {
                // empty catch block
            }
        }
        if ((lp = this.getServer().getPluginManager().getPlugin("LuckPerms")) != null && lp.isEnabled()) {
            this.getLogger().info("LuckPerms integration active: All Moonlight permissions and groups are registered and manageable via LuckPerms.");
        }
        if ((mv = this.getServer().getPluginManager().getPlugin("Multiverse-Core")) != null && mv.isEnabled()) {
            this.getLogger().info("Multiverse-Core integration active: Per-world latest location tracking and spawn-on-join enabled.");
        }
        long loadMs = System.currentTimeMillis() - startTime;
        this.getLogger().info("MoonlightVanillaSurvivalCore successfully loaded in " + loadMs + "ms.");
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        });
    }

    public void onDisable() {
        this.getLogger().info("Shutting down MoonlightVanillaSurvivalCore...");
        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }
        if (this.auditManager != null) {
            this.auditManager.shutdown();
        }
        if (this.teleportManager != null) {
            this.teleportManager.cancelAllPending();
        }
        if (this.playerVaultManager != null) {
            this.playerVaultManager.saveAllOnShutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        if (this.proxyManager != null) {
            this.proxyManager.shutdown();
        }
        this.getLogger().info("MoonlightVanillaSurvivalCore safely disabled.");
    }

    private void registerCommands() {
        this.setHomeCommand = new SetHomeCommand(this);
        this.homeCommand = new HomeCommand(this);
        this.delHomeCommand = new DelHomeCommand(this);
        this.registerSafeCommand("sethome", "Set a personal home point", Collections.emptyList(), this.setHomeCommand, this.setHomeCommand, null);
        this.registerSafeCommand("home", "Teleport to your home", Collections.emptyList(), this.homeCommand, this.homeCommand, null);
        this.registerSafeCommand("delhome", "Delete or remove a personal home point", List.of("removehome", "deletehome", "rmhome", "remhome", "removehomes", "delhomes"), this.delHomeCommand, this.delHomeCommand, null);
        this.registerSafeCommand("removehome", "Remove or delete a personal home point", List.of("delhome", "deletehome", "rmhome", "remhome", "removehomes", "delhomes"), this.delHomeCommand, this.delHomeCommand, null);
        this.registerSafeCommand("homes", "List all personal homes", Collections.emptyList(), this.homeCommand, this.homeCommand, null);
        FeaturesModule featuresModule = new FeaturesModule(this);
        this.registerSafeCommand("wb", "Open workbench", List.of("workbench"), featuresModule, null, null);
        this.registerSafeCommand("hat", "Hat command", Collections.emptyList(), featuresModule, null, null);
        this.registerSafeCommand("repair", "Repair item", Collections.emptyList(), featuresModule, null, null);
        this.playerVaultManager = new PlayerVaultManager(this);
        this.playerVaultCommand = new PlayerVaultCommand(this, this.playerVaultManager);
        this.registerSafeCommand("pv", "Player vault", List.of("playervault", "vault", "mpv"), this.playerVaultCommand, this.playerVaultCommand, null);
        this.registerSafeCommand("pv1", "Open Player Vault 1", List.of("playervault1", "vault1", "mpv1"), this.playerVaultCommand, this.playerVaultCommand, null);
        this.registerSafeCommand("pv2", "Open Player Vault 2", List.of("playervault2", "vault2", "mpv2"), this.playerVaultCommand, this.playerVaultCommand, null);
        this.registerSafeCommand("pv3", "Open Player Vault 3", List.of("playervault3", "vault3", "mpv3"), this.playerVaultCommand, this.playerVaultCommand, null);
        this.registerSafeCommand("pv4", "Open Player Vault 4", List.of("playervault4", "vault4", "mpv4"), this.playerVaultCommand, this.playerVaultCommand, null);
        this.registerSafeCommand("pv5", "Open Player Vault 5", List.of("playervault5", "vault5", "mpv5"), this.playerVaultCommand, this.playerVaultCommand, null);
        this.changeNameCommand = new ChangeNameCommand(this);
        this.registerSafeCommand("changename", "Change or reset player display nickname", List.of("mchangename", "setname", "nickplayer"), this.changeNameCommand, this.changeNameCommand, null);
        this.registerSafeCommand("setname", "Change or reset player display nickname", Collections.emptyList(), this.changeNameCommand, this.changeNameCommand, null);
        StaffHelpCommand staffHelpCommand = new StaffHelpCommand(this);
        this.registerSafeCommand("shelp", "Staff command handbook and administrative operational guide", List.of("staffhelp", "mshelp"), staffHelpCommand, staffHelpCommand, null);
        BedrockCommand bedrockCommand = new BedrockCommand(this);
        this.registerSafeCommand("bedrock", "Bedrock crossplay touch GUI", List.of("bedrockgui", "mbedrock", "bgui"), bedrockCommand, bedrockCommand, null);
        ViaInfoCommand viaInfoCommand = new ViaInfoCommand(this);
        this.registerSafeCommand("viainfo", "Display multi-version client protocol and ViaBackwards bridge state", List.of("mviainfo", "viastatus", "mvia"), viaInfoCommand, viaInfoCommand, null);
        this.logsCommand = new LogsCommand(this);
        this.registerSafeCommand("logs", "Audit logging, player history, and snapshot inspector", List.of("mlogs", "auditlogs", "paperlogs", "log"), this.logsCommand, this.logsCommand, null);
        BanCommand banCommand = new BanCommand(this);
        this.registerSafeCommand("ban", "Ban a player with timed duration and Ban ID", Collections.emptyList(), banCommand, banCommand, "moonlight.staff.ban");
        UnbanCommand unbanCommand = new UnbanCommand(this);
        this.registerSafeCommand("unban", "Pardon a player ban", Collections.emptyList(), unbanCommand, unbanCommand, "moonlight.staff.unban");
        BanInfoCommand banInfoCommand = new BanInfoCommand(this);
        this.registerSafeCommand("baninfo", "Display ban info and dynamic live timer", Collections.emptyList(), banInfoCommand, banInfoCommand, "moonlight.staff.baninfo");
        KickCommand kickCommand = new KickCommand(this);
        this.registerSafeCommand("kick", "Kick a player from the server with warning reason", Collections.emptyList(), kickCommand, kickCommand, "moonlight.staff.kick");
        MuteCommand muteCommand = new MuteCommand(this);
        this.registerSafeCommand("mute", "Mute a player", Collections.emptyList(), muteCommand, null, "moonlight.staff.mute");
        UnmuteCommand unmuteCommand = new UnmuteCommand(this);
        this.registerSafeCommand("unmute", "Unmute a player", Collections.emptyList(), unmuteCommand, null, "moonlight.staff.mute");
        this.scoreboardCommand = new ScoreboardCommand(this);
        this.registerSafeCommand("scoreboard", "Toggle or view personal sidebar scoreboard", List.of("sb", "sbtoggle"), this.scoreboardCommand, this.scoreboardCommand, null);
        this.registerSafeCommand("sbtoggle", "Toggle personal sidebar scoreboard", Collections.emptyList(), this.scoreboardCommand, this.scoreboardCommand, null);
        this.registerSafeCommand("sb", "Toggle personal sidebar scoreboard", Collections.emptyList(), this.scoreboardCommand, this.scoreboardCommand, null);
        this.messageCommand = new MessageCommand(this);
        this.registerSafeCommand("msg", "Send a private direct message to a player", List.of("w", "tell", "whisper", "pm", "m", "t"), this.messageCommand, this.messageCommand, null);
        this.replyCommand = new ReplyCommand(this, this.messageCommand);
        this.registerSafeCommand("reply", "Quickly reply to the last player who messaged you", List.of("r"), this.replyCommand, this.replyCommand, null);
        this.registerSafeCommand("r", "Quickly reply to the last player who messaged you", Collections.emptyList(), this.replyCommand, this.replyCommand, null);
        this.invseeCommand = new InvseeCommand(this);
        this.registerSafeCommand("inv", "Inspect a player's inventory live", List.of("invsee"), this.invseeCommand, this.invseeCommand, null);
        this.registerSafeCommand("invsee", "Inspect a player's inventory live", Collections.emptyList(), this.invseeCommand, this.invseeCommand, null);
        EnderChestCommand enderChestCommand = new EnderChestCommand(this);
        this.registerSafeCommand("ec", "Open enderchest", List.of("enderchest"), enderChestCommand, enderChestCommand, null);
        if (this.proxyManager != null) {
            HubCommand hubCommand = new HubCommand(this, this.proxyManager);
            this.registerSafeCommand("hub", "Connect to network lobby via Velocity or teleport to spawn in standalone Paper", List.of("lobby"), hubCommand, hubCommand, "moonlight.hub.use");
            ServerCommand serverCommand = new ServerCommand(this, this.proxyManager);
            this.registerSafeCommand("server", "Switch backend servers via Velocity proxy or check server status", List.of("switchserver"), serverCommand, serverCommand, "moonlight.server.use");
        }
        if (this.wrapManager != null && this.wrapGui != null) {
            this.wrapCommand = new WrapCommand(this, this.wrapManager, this.wrapGui);
            this.registerSafeCommand("itemwarps", "Browse and apply 3D weapon wraps", List.of("itemwraps", "itemwarp", "itemwrap", "wraps", "wrap", "mitemwarps", "mitemwarp", "mwraps", "mwrap"), this.wrapCommand, this.wrapCommand, null);
            this.registerSafeCommand("itemwraps", "Browse and apply 3D weapon wraps", Collections.emptyList(), this.wrapCommand, this.wrapCommand, null);
            this.registerSafeCommand("itemwarp", "Browse and apply 3D weapon wraps", Collections.emptyList(), this.wrapCommand, this.wrapCommand, null);
            this.registerSafeCommand("itemwrap", "Browse and apply 3D weapon wraps", Collections.emptyList(), this.wrapCommand, this.wrapCommand, null);
            this.registerSafeCommand("wraps", "Browse and apply 3D weapon wraps", List.of("wrap"), this.wrapCommand, this.wrapCommand, null);
            this.registerSafeCommand("wrap", "Browse and apply 3D weapon wraps", Collections.emptyList(), this.wrapCommand, this.wrapCommand, null);
        }
        this.linkCommand = new LinkCommand(this);
        this.registerSafeCommand("weblink", "View official website link", List.of("link", "website", "site", "web", "mwebsite", "mweblink"), this.linkCommand, this.linkCommand, "moonlight.link");
        this.registerSafeCommand("link", "View official website link", Collections.emptyList(), this.linkCommand, this.linkCommand, "moonlight.link");
        this.registerSafeCommand("website", "View official website link", List.of("mwebsite"), this.linkCommand, this.linkCommand, "moonlight.link");
        this.registerSafeCommand("discord", "Join official Discord community", List.of("dc", "dclink", "discordlink", "mdiscord"), this.linkCommand, this.linkCommand, "moonlight.discord");
        this.registerSafeCommand("dc", "Join official Discord community", Collections.emptyList(), this.linkCommand, this.linkCommand, "moonlight.discord");
        this.registerSafeCommand("dclink", "Join official Discord community", Collections.emptyList(), this.linkCommand, this.linkCommand, "moonlight.discord");
        this.registerSafeCommand("mdiscord", "Join official Discord community", Collections.emptyList(), this.linkCommand, this.linkCommand, "moonlight.discord");
        this.youTubeCommand = new YouTubeCommand(this);
        this.registerSafeCommand("yt", "List server YouTubers and content creators", List.of("youtube", "videos", "youtubers", "myoutube"), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("youtube", "List server YouTubers and content creators", List.of("myoutube", "youtubers"), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("videos", "List server YouTubers and content creators", Collections.emptyList(), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("myoutube", "List server YouTubers and content creators", Collections.emptyList(), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("addyoutube", "Add a YouTube channel link (OP only)", List.of("addyt"), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("addyt", "Add a YouTube channel link (OP only)", Collections.emptyList(), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("removeyoutube", "Remove a YouTube channel link (OP only)", List.of("removeyt", "delyoutube", "delyt", "removelink"), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("removeyt", "Remove a YouTube channel link (OP only)", Collections.emptyList(), this.youTubeCommand, this.youTubeCommand, null);
        this.registerSafeCommand("ytpriority", "Set priority rank for server YouTubers (OP only)", List.of("setytpriority"), this.youTubeCommand, this.youTubeCommand, null);
        this.helpCommand = new HelpCommand(this);
        this.registerSafeCommand("help", "Moonlight SMP player guide and comprehensive command handbook", List.of("mhelp", "guide", "playerguide"), this.helpCommand, this.helpCommand, null);
        this.rulesCommand = new RulesCommand(this);
        this.registerSafeCommand("rules", "Open the official interactive server rulebook", List.of("mrules", "rulebook", "mrulebook"), this.rulesCommand, this.rulesCommand, null);
        this.registerSafeCommand("rulebook", "Open the official interactive server rulebook", List.of("mrulebook"), this.rulesCommand, this.rulesCommand, null);
        this.registerSafeCommand("ranks", "Open interactive GUI showing all paid supporter ranks, perks, and costs", List.of("rank", "store", "buy", "paidranks", "donateranks", "donorranks"), this.ranksCommand, this.ranksCommand, "moonlight.ranks");
        this.registerSafeCommand("rank", "Open interactive GUI showing all paid supporter ranks, perks, and costs", Collections.emptyList(), this.ranksCommand, this.ranksCommand, "moonlight.ranks");
        this.registerSafeCommand("store", "Open interactive server store and ranks GUI", List.of("buy", "webstore"), this.ranksCommand, this.ranksCommand, "moonlight.ranks");
        this.registerSafeCommand("buy", "Open interactive server store and ranks GUI", Collections.emptyList(), this.ranksCommand, this.ranksCommand, "moonlight.ranks");
        ResourcePackCommand rpCommand = new ResourcePackCommand(this, this.resourcePackManager);
        this.registerSafeCommand("resourcepack", "Load or reload server resource pack with SHA-1 cache support", List.of("rp", "mrp"), rpCommand, rpCommand, null);
        this.registerSafeCommand("rp", "Load or reload server resource pack with SHA-1 cache support", Collections.emptyList(), rpCommand, rpCommand, null);
    }

    private void forceRegisterCommand(String name, String description, List<String> aliases, final CommandExecutor executor, final TabCompleter tabCompleter, String permission) {
        try {
            Command existing = Bukkit.getCommandMap().getCommand(name);
            if (existing != null) {
                existing.unregister(Bukkit.getCommandMap());
            }
            Command bukkitCmd = new Command(name, description, "/" + name, aliases != null ? aliases : Collections.emptyList()) {

                public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                    if (this.getPermission() != null && !this.getPermission().isEmpty() && !this.testPermission(sender)) {
                        return true;
                    }
                    return executor.onCommand(sender, (Command)this, commandLabel, args);
                }

                @NotNull
                public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    List list;
                    if (tabCompleter != null && (list = tabCompleter.onTabComplete(sender, (Command)this, alias, args)) != null) {
                        return list;
                    }
                    return super.tabComplete(sender, alias, args);
                }
            };
            if (permission != null) {
                bukkitCmd.setPermission(permission);
                bukkitCmd.setPermissionMessage(MessageUtil.toLegacy(MessageUtil.format("&9MoonLight &8\u2759 &cYou are forbidden to use this.")));
            }
            Bukkit.getCommandMap().register("moonlight", bukkitCmd);
        }
        catch (Exception e) {
            this.getLogger().warning("Could not force register /" + name + ": " + e.getMessage());
        }
    }

    private void registerSafeCommand(String name, String description, List<String> aliases, final CommandExecutor executor, final TabCompleter tabCompleter, String permission) {
        try {
            String mAlias;
            Plugin owningPlugin;
            PluginCommand pluginCmd = this.getCommand(name);
            if (pluginCmd != null) {
                pluginCmd.setExecutor(executor);
                if (tabCompleter != null) {
                    pluginCmd.setTabCompleter(tabCompleter);
                } else if (executor instanceof TabCompleter) {
                    TabCompleter tc = (TabCompleter)executor;
                    pluginCmd.setTabCompleter(tc);
                }
                if (permission != null && !permission.isEmpty()) {
                    pluginCmd.setPermission(permission);
                    pluginCmd.setPermissionMessage(MessageUtil.toLegacy(MessageUtil.format("&9MoonLight &8\u2759 &cYou are forbidden to use this.")));
                }
                return;
            }
            Command existing = Bukkit.getCommandMap().getCommand(name);
            String regName = name;
            if (existing instanceof PluginIdentifiableCommand && (owningPlugin = ((PluginIdentifiableCommand)existing).getPlugin()) != null && !owningPlugin.equals(this)) {
                this.getLogger().info("External plugin '" + owningPlugin.getName() + "' already registered /" + name + ". Moonlight is mapping /m" + name + " to prevent collision.");
                regName = "m" + name;
            }
            List<String> safeAliases = new ArrayList<>();
            if (aliases != null) {
                for (String alias : aliases) {
                    Plugin owner;
                    Command existingAlias = Bukkit.getCommandMap().getCommand(alias);
                    if (existingAlias == null) {
                        safeAliases.add(alias);
                        continue;
                    }
                    if (!(existingAlias instanceof PluginIdentifiableCommand) || (owner = ((PluginIdentifiableCommand)existingAlias).getPlugin()) != null && !owner.equals(this)) continue;
                    safeAliases.add(alias);
                }
            }
            mAlias = "m" + name;
            if (!safeAliases.contains(mAlias) && !regName.equals(mAlias)) {
                safeAliases.add(mAlias);
            }
            Command bukkitCmd = new Command(regName, description, "/" + regName, safeAliases) {

                public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                    if (this.getPermission() != null && !this.getPermission().isEmpty() && !this.testPermission(sender)) {
                        return true;
                    }
                    return executor.onCommand(sender, (Command)this, commandLabel, args);
                }

                @NotNull
                public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    TabCompleter tc;
                    List list;
                    if (tabCompleter != null) {
                        List list2 = tabCompleter.onTabComplete(sender, (Command)this, alias, args);
                        if (list2 != null) {
                            return list2;
                        }
                    } else if (executor instanceof TabCompleter && (list = (tc = (TabCompleter)executor).onTabComplete(sender, (Command)this, alias, args)) != null) {
                        return list;
                    }
                    return super.tabComplete(sender, alias, args);
                }
            };
            if (permission != null) {
                bukkitCmd.setPermission(permission);
                bukkitCmd.setPermissionMessage(MessageUtil.toLegacy(MessageUtil.format("&9MoonLight &8\u2759 &cYou are forbidden to use this.")));
            }
            Bukkit.getCommandMap().register("moonlight", bukkitCmd);
        }
        catch (Exception e) {
            this.getLogger().warning("Could not register command /" + name + " via CommandMap: " + e.getMessage());
        }
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents((Listener)new TeleportListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.securityAuditor, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BanLoginListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PluginHiderListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ChatAndJoinListener(this, this.messageCommand), (Plugin)this);
        if (this.viaBackwardsManager != null) {
            this.getServer().getPluginManager().registerEvents((Listener)this.viaBackwardsManager, (Plugin)this);
        }
        if (this.wrapManager != null && this.wrapGui != null) {
            this.getServer().getPluginManager().registerEvents((Listener)new WrapListener(this, this.wrapManager, this.wrapGui), (Plugin)this);
        }
        this.getServer().getPluginManager().registerEvents((Listener)new WorldLocationListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new HelpAndRulesOverrideListener(this, this.helpCommand, this.rulesCommand, this.linkCommand, this.youTubeCommand, this.ranksCommand, this.invseeCommand, this.wrapCommand, this.scoreboardCommand, this.changeNameCommand, this.homeCommand, this.setHomeCommand, this.delHomeCommand, this.logsCommand, this.messageCommand, this.replyCommand, this.playerVaultCommand), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new RanksListener(this, this.ranksManager), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GuiSecurityListener(this), (Plugin)this);
        VanishUtil.registerVanishHook(this);
    }

    public static MoonlightVanillaSurvivalCore getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public HomeManager getHomeManager() {
        return this.homeManager;
    }

    public TeleportManager getTeleportManager() {
        return this.teleportManager;
    }

    public SecurityAuditor getSecurityAuditor() {
        return this.securityAuditor;
    }

    public BedrockManager getBedrockManager() {
        return this.bedrockManager;
    }

    public AuditManager getAuditManager() {
        return this.auditManager;
    }

    public BanManager getBanManager() {
        return this.banManager;
    }

    public ViaBackwardsManager getViaBackwardsManager() {
        return this.viaBackwardsManager;
    }

    public MuteManager getMuteManager() {
        return this.muteManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    public ResourcePackManager getResourcePackManager() {
        return this.resourcePackManager;
    }

    public MessageCommand getMessageCommand() {
        return this.messageCommand;
    }

    public ProxyManager getProxyManager() {
        return this.proxyManager;
    }

    public WrapManager getWrapManager() {
        return this.wrapManager;
    }

    public WrapGui getWrapGui() {
        return this.wrapGui;
    }

    public WorldLocationManager getWorldLocationManager() {
        return this.worldLocationManager;
    }

    public SpawnManager getSpawnManager() {
        return this.spawnManager;
    }

    public HelpCommand getHelpCommand() {
        return this.helpCommand;
    }

    public RulesCommand getRulesCommand() {
        return this.rulesCommand;
    }

    public RanksManager getRanksManager() {
        return this.ranksManager;
    }

    public RanksGui getRanksGui() {
        return this.ranksGui;
    }

    public RanksCommand getRanksCommand() {
        return this.ranksCommand;
    }

    public PlayerVaultManager getPlayerVaultManager() {
        return this.playerVaultManager;
    }

    public PlayerVaultCommand getPlayerVaultCommand() {
        return this.playerVaultCommand;
    }

    public ReplyCommand getReplyCommand() {
        return this.replyCommand;
    }
}

