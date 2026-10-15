package com.moonlight.vanilla;

import com.moonlight.vanilla.commands.*;
import com.moonlight.vanilla.database.DatabaseManager;
import com.moonlight.vanilla.modules.audit.AuditManager;
import com.moonlight.vanilla.modules.audit.commands.LogsCommand;
import com.moonlight.vanilla.modules.chat.commands.MessageCommand;
import com.moonlight.vanilla.modules.chat.commands.ReplyCommand;
import com.moonlight.vanilla.modules.chat.listeners.ChatAndJoinListener;
import com.moonlight.vanilla.modules.crossplay.*;
import com.moonlight.vanilla.modules.features.FeaturesModule;
import com.moonlight.vanilla.modules.gui.GuiSecurityListener;
import com.moonlight.vanilla.modules.homes.HomeManager;
import com.moonlight.vanilla.modules.homes.commands.*;
import com.moonlight.vanilla.modules.proxy.ProxyManager;
import com.moonlight.vanilla.modules.proxy.commands.HubCommand;
import com.moonlight.vanilla.modules.proxy.commands.ServerCommand;
import com.moonlight.vanilla.modules.punishments.*;
import com.moonlight.vanilla.modules.ranks.*;
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
import com.moonlight.vanilla.modules.wraps.*;
import com.moonlight.vanilla.security.SecurityAuditor;
import com.moonlight.vanilla.utils.MessageUtil;
import com.moonlight.vanilla.utils.VanishUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MoonlightVanillaSurvivalCore
 * Central core engine for Moonlight Vanilla Survival (Paper 26.2 / MC 1.21.4).
 */
public final class MoonlightVanillaSurvivalCore extends JavaPlugin {

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

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        getLogger().info("=================================================");
        getLogger().info("Starting MoonlightVanillaSurvivalCore (Paper 26.2)");
        getLogger().info("=================================================");
        saveDefaultConfig();

        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            getLogger().info("[Database] HikariCP connection pool active and ready.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database pool. Disabling plugin to prevent data corruption!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        securityAuditor = new SecurityAuditor(this);
        securityAuditor.enforceStartupAudit();

        homeManager = new HomeManager(this);
        teleportManager = new TeleportManager(this);
        bedrockManager = new BedrockManager(this);
        bedrockManager.initialize();
        viaBackwardsManager = new ViaBackwardsManager(this);
        viaBackwardsManager.initialize();
        auditManager = new AuditManager(this);
        auditManager.initialize();
        banManager = new BanManager(this);
        banManager.initialize();
        muteManager = new MuteManager(this);
        muteManager.initialize();
        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.initialize();
        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.initialize();
        proxyManager = new ProxyManager(this);
        proxyManager.initialize();
        wrapManager = new WrapManager(this);
        wrapManager.initialize();
        wrapGui = new WrapGui(this, wrapManager);
        worldLocationManager = new WorldLocationManager(this);
        spawnManager = new SpawnManager(this);

        ranksManager = new RanksManager(this);
        ranksManager.initialize();
        ranksGui = new RanksGui(this, ranksManager);
        ranksCommand = new RanksCommand(this, ranksManager, ranksGui);

        registerCommands();
        registerListeners();

        if (!new File(getDataFolder(), "moonlight_logo.png").exists()) {
            try { saveResource("moonlight_logo.png", false); } catch (Exception ignored) {}
        }

        Plugin lp = getServer().getPluginManager().getPlugin("LuckPerms");
        if (lp != null && lp.isEnabled()) {
            getLogger().info("LuckPerms integration active: All Moonlight permissions and groups are registered and manageable via LuckPerms.");
        }
        Plugin mv = getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (mv != null && mv.isEnabled()) {
            getLogger().info("Multiverse-Core integration active: Per-world latest location tracking and spawn-on-join enabled.");
        }

        long loadMs = System.currentTimeMillis() - startTime;
        getLogger().info("MoonlightVanillaSurvivalCore successfully loaded in " + loadMs + "ms.");
        Bukkit.getScheduler().runTask(this, () -> Bukkit.getOnlinePlayers().forEach(Player::updateCommands));
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down MoonlightVanillaSurvivalCore...");
        if (scoreboardManager != null) scoreboardManager.shutdown();
        if (auditManager != null) auditManager.shutdown();
        if (teleportManager != null) teleportManager.cancelAllPending();
        if (playerVaultManager != null) playerVaultManager.saveAllOnShutdown();
        if (databaseManager != null) databaseManager.shutdown();
        if (proxyManager != null) proxyManager.shutdown();
        getLogger().info("MoonlightVanillaSurvivalCore safely disabled.");
    }

    private record CmdReg(String name, String desc, List<String> aliases, CommandExecutor exec, TabCompleter tab, String perm) {
        CmdReg(String name, String desc, CommandExecutor exec) {
            this(name, desc, List.of(), exec, exec instanceof TabCompleter tc ? tc : null, null);
        }
        CmdReg(String name, String desc, List<String> aliases, CommandExecutor exec) {
            this(name, desc, aliases, exec, exec instanceof TabCompleter tc ? tc : null, null);
        }
        CmdReg(String name, String desc, List<String> aliases, CommandExecutor exec, String perm) {
            this(name, desc, aliases, exec, exec instanceof TabCompleter tc ? tc : null, perm);
        }
        CmdReg(String name, String desc, CommandExecutor exec, String perm) {
            this(name, desc, List.of(), exec, exec instanceof TabCompleter tc ? tc : null, perm);
        }
    }

    private void registerCommands() {
        setHomeCommand = new SetHomeCommand(this);
        homeCommand = new HomeCommand(this);
        delHomeCommand = new DelHomeCommand(this);
        FeaturesModule features = new FeaturesModule(this);
        playerVaultManager = new PlayerVaultManager(this);
        playerVaultCommand = new PlayerVaultCommand(this, playerVaultManager);
        changeNameCommand = new ChangeNameCommand(this);
        StaffHelpCommand staffHelp = new StaffHelpCommand(this);
        BedrockCommand bedrock = new BedrockCommand(this);
        ViaInfoCommand viaInfo = new ViaInfoCommand(this);
        logsCommand = new LogsCommand(this);
        BanCommand ban = new BanCommand(this);
        UnbanCommand unban = new UnbanCommand(this);
        BanInfoCommand banInfo = new BanInfoCommand(this);
        KickCommand kick = new KickCommand(this);
        MuteCommand mute = new MuteCommand(this);
        UnmuteCommand unmute = new UnmuteCommand(this);
        scoreboardCommand = new ScoreboardCommand(this);
        messageCommand = new MessageCommand(this);
        replyCommand = new ReplyCommand(this, messageCommand);
        invseeCommand = new InvseeCommand(this);
        EnderChestCommand enderChest = new EnderChestCommand(this);
        linkCommand = new LinkCommand(this);
        youTubeCommand = new YouTubeCommand(this);
        helpCommand = new HelpCommand(this);
        rulesCommand = new RulesCommand(this);
        ResourcePackCommand rp = new ResourcePackCommand(this, resourcePackManager);

        List<String> delHomeAliases = List.of("removehome", "deletehome", "rmhome", "remhome", "removehomes", "delhomes");
        List<CmdReg> commands = new ArrayList<>(List.of(
                new CmdReg("sethome", "Set a personal home point", setHomeCommand),
                new CmdReg("home", "Teleport to your home", homeCommand),
                new CmdReg("delhome", "Delete or remove a personal home point", delHomeAliases, delHomeCommand),
                new CmdReg("removehome", "Remove or delete a personal home point", delHomeAliases, delHomeCommand),
                new CmdReg("homes", "List all personal homes", homeCommand),
                new CmdReg("wb", "Open workbench", List.of("workbench"), features),
                new CmdReg("hat", "Hat command", features),
                new CmdReg("repair", "Repair item", features),
                new CmdReg("pv", "Player vault", List.of("playervault", "vault", "mpv"), playerVaultCommand),
                new CmdReg("pv1", "Open Player Vault 1", List.of("playervault1", "vault1", "mpv1"), playerVaultCommand),
                new CmdReg("pv2", "Open Player Vault 2", List.of("playervault2", "vault2", "mpv2"), playerVaultCommand),
                new CmdReg("pv3", "Open Player Vault 3", List.of("playervault3", "vault3", "mpv3"), playerVaultCommand),
                new CmdReg("pv4", "Open Player Vault 4", List.of("playervault4", "vault4", "mpv4"), playerVaultCommand),
                new CmdReg("pv5", "Open Player Vault 5", List.of("playervault5", "vault5", "mpv5"), playerVaultCommand),
                new CmdReg("changename", "Change or reset player display nickname", List.of("mchangename", "setname", "nickplayer"), changeNameCommand),
                new CmdReg("setname", "Change or reset player display nickname", changeNameCommand),
                new CmdReg("shelp", "Staff command handbook and administrative operational guide", List.of("staffhelp", "mshelp"), staffHelp),
                new CmdReg("bedrock", "Bedrock crossplay touch GUI", List.of("bedrockgui", "mbedrock", "bgui"), bedrock),
                new CmdReg("viainfo", "Display multi-version client protocol and ViaBackwards bridge state", List.of("mviainfo", "viastatus", "mvia"), viaInfo),
                new CmdReg("logs", "Audit logging, player history, and snapshot inspector", List.of("mlogs", "auditlogs", "paperlogs", "log"), logsCommand),
                new CmdReg("ban", "Ban a player with timed duration and Ban ID", List.of(), ban, "moonlight.staff.ban"),
                new CmdReg("unban", "Pardon a player ban", List.of(), unban, "moonlight.staff.unban"),
                new CmdReg("baninfo", "Display ban info and dynamic live timer", List.of(), banInfo, "moonlight.staff.baninfo"),
                new CmdReg("kick", "Kick a player from the server with warning reason", List.of(), kick, "moonlight.staff.kick"),
                new CmdReg("mute", "Mute a player", List.of(), mute, null, "moonlight.staff.mute"),
                new CmdReg("unmute", "Unmute a player", List.of(), unmute, null, "moonlight.staff.mute"),
                new CmdReg("scoreboard", "Toggle or view personal sidebar scoreboard", List.of("sb", "sbtoggle"), scoreboardCommand),
                new CmdReg("sbtoggle", "Toggle personal sidebar scoreboard", scoreboardCommand),
                new CmdReg("sb", "Toggle personal sidebar scoreboard", scoreboardCommand),
                new CmdReg("msg", "Send a private direct message to a player", List.of("w", "tell", "whisper", "pm", "m", "t"), messageCommand),
                new CmdReg("reply", "Quickly reply to the last player who messaged you", List.of("r"), replyCommand),
                new CmdReg("r", "Quickly reply to the last player who messaged you", replyCommand),
                new CmdReg("inv", "Inspect a player's inventory live", List.of("invsee"), invseeCommand),
                new CmdReg("invsee", "Inspect a player's inventory live", invseeCommand),
                new CmdReg("ec", "Open enderchest", List.of("enderchest"), enderChest),
                new CmdReg("weblink", "View official website link", List.of("link", "website", "site", "web", "mwebsite", "mweblink"), linkCommand, "moonlight.link"),
                new CmdReg("link", "View official website link", List.of(), linkCommand, "moonlight.link"),
                new CmdReg("website", "View official website link", List.of("mwebsite"), linkCommand, "moonlight.link"),
                new CmdReg("discord", "Join official Discord community", List.of("dc", "dclink", "discordlink", "mdiscord"), linkCommand, "moonlight.discord"),
                new CmdReg("dc", "Join official Discord community", List.of(), linkCommand, "moonlight.discord"),
                new CmdReg("dclink", "Join official Discord community", List.of(), linkCommand, "moonlight.discord"),
                new CmdReg("mdiscord", "Join official Discord community", List.of(), linkCommand, "moonlight.discord"),
                new CmdReg("yt", "List server YouTubers and content creators", List.of("youtube", "videos", "youtubers", "myoutube"), youTubeCommand),
                new CmdReg("youtube", "List server YouTubers and content creators", List.of("myoutube", "youtubers"), youTubeCommand),
                new CmdReg("videos", "List server YouTubers and content creators", youTubeCommand),
                new CmdReg("myoutube", "List server YouTubers and content creators", youTubeCommand),
                new CmdReg("addyoutube", "Add a YouTube channel link (OP only)", List.of("addyt"), youTubeCommand),
                new CmdReg("addyt", "Add a YouTube channel link (OP only)", youTubeCommand),
                new CmdReg("removeyoutube", "Remove a YouTube channel link (OP only)", List.of("removeyt", "delyoutube", "delyt", "removelink"), youTubeCommand),
                new CmdReg("removeyt", "Remove a YouTube channel link (OP only)", youTubeCommand),
                new CmdReg("ytpriority", "Set priority rank for server YouTubers (OP only)", List.of("setytpriority"), youTubeCommand),
                new CmdReg("help", "Moonlight SMP player guide and comprehensive command handbook", List.of("mhelp", "guide", "playerguide"), helpCommand),
                new CmdReg("rules", "Open the official interactive server rulebook", List.of("mrules", "rulebook", "mrulebook"), rulesCommand),
                new CmdReg("rulebook", "Open the official interactive server rulebook", List.of("mrulebook"), rulesCommand),
                new CmdReg("ranks", "Open interactive GUI showing all paid supporter ranks, perks, and costs", List.of("rank", "store", "buy", "paidranks", "donateranks", "donorranks"), ranksCommand, "moonlight.ranks"),
                new CmdReg("rank", "Open interactive GUI showing all paid supporter ranks, perks, and costs", List.of(), ranksCommand, "moonlight.ranks"),
                new CmdReg("store", "Open interactive server store and ranks GUI", List.of("buy", "webstore"), ranksCommand, "moonlight.ranks"),
                new CmdReg("buy", "Open interactive server store and ranks GUI", List.of(), ranksCommand, "moonlight.ranks"),
                new CmdReg("resourcepack", "Load or reload server resource pack with SHA-1 cache support", List.of("rp", "mrp"), rp),
                new CmdReg("rp", "Load or reload server resource pack with SHA-1 cache support", rp)
        ));

        if (proxyManager != null) {
            HubCommand hub = new HubCommand(this, proxyManager);
            commands.add(new CmdReg("hub", "Connect to network lobby via Velocity or teleport to spawn in standalone Paper", List.of("lobby"), hub, "moonlight.hub.use"));
            ServerCommand server = new ServerCommand(this, proxyManager);
            commands.add(new CmdReg("server", "Switch backend servers via Velocity proxy or check server status", List.of("switchserver"), server, "moonlight.server.use"));
        }

        if (wrapManager != null && wrapGui != null) {
            wrapCommand = new WrapCommand(this, wrapManager, wrapGui);
            commands.add(new CmdReg("itemwarps", "Browse and apply 3D weapon wraps", List.of("itemwraps", "itemwarp", "itemwrap", "wraps", "wrap", "mitemwarps", "mitemwarp", "mwraps", "mwrap"), wrapCommand));
            commands.add(new CmdReg("itemwraps", "Browse and apply 3D weapon wraps", wrapCommand));
            commands.add(new CmdReg("itemwarp", "Browse and apply 3D weapon wraps", wrapCommand));
            commands.add(new CmdReg("itemwrap", "Browse and apply 3D weapon wraps", wrapCommand));
            commands.add(new CmdReg("wraps", "Browse and apply 3D weapon wraps", List.of("wrap"), wrapCommand));
            commands.add(new CmdReg("wrap", "Browse and apply 3D weapon wraps", wrapCommand));
        }

        for (CmdReg c : commands) {
            registerSafeCommand(c.name(), c.desc(), c.aliases(), c.exec(), c.tab(), c.perm());
        }
    }

    private void registerSafeCommand(String name, String description, List<String> aliases, CommandExecutor executor, TabCompleter tabCompleter, String permission) {
        try {
            PluginCommand pluginCmd = getCommand(name);
            if (pluginCmd != null) {
                pluginCmd.setExecutor(executor);
                if (tabCompleter != null) {
                    pluginCmd.setTabCompleter(tabCompleter);
                } else if (executor instanceof TabCompleter tc) {
                    pluginCmd.setTabCompleter(tc);
                }
                if (permission != null && !permission.isEmpty()) {
                    pluginCmd.setPermission(permission);
                    pluginCmd.setPermissionMessage(MessageUtil.toLegacy(MessageUtil.format("&9MoonLight &8❙ &cYou are forbidden to use this.")));
                }
                return;
            }

            Command existing = Bukkit.getCommandMap().getCommand(name);
            String regName = name;
            if (existing instanceof PluginIdentifiableCommand pic && pic.getPlugin() != null && !pic.getPlugin().equals(this)) {
                getLogger().info("External plugin '" + pic.getPlugin().getName() + "' already registered /" + name + ". Moonlight is mapping /m" + name + " to prevent collision.");
                regName = "m" + name;
            }

            List<String> safeAliases = new ArrayList<>();
            if (aliases != null) {
                for (String alias : aliases) {
                    Command existingAlias = Bukkit.getCommandMap().getCommand(alias);
                    if (existingAlias == null || (existingAlias instanceof PluginIdentifiableCommand pic && this.equals(pic.getPlugin()))) {
                        safeAliases.add(alias);
                    }
                }
            }

            String mAlias = "m" + name;
            if (!safeAliases.contains(mAlias) && !regName.equals(mAlias)) {
                safeAliases.add(mAlias);
            }

            Command bukkitCmd = new Command(regName, description, "/" + regName, safeAliases) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                    if (getPermission() != null && !getPermission().isEmpty() && !testPermission(sender)) {
                        return true;
                    }
                    return executor.onCommand(sender, this, commandLabel, args);
                }

                @Override
                public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    if (tabCompleter != null) {
                        List<String> list = tabCompleter.onTabComplete(sender, this, alias, args);
                        if (list != null) return list;
                    } else if (executor instanceof TabCompleter tc) {
                        List<String> list = tc.onTabComplete(sender, this, alias, args);
                        if (list != null) return list;
                    }
                    return super.tabComplete(sender, alias, args);
                }
            };

            if (permission != null) {
                bukkitCmd.setPermission(permission);
                bukkitCmd.setPermissionMessage(MessageUtil.toLegacy(MessageUtil.format("&9MoonLight &8❙ &cYou are forbidden to use this.")));
            }
            Bukkit.getCommandMap().register("moonlight", bukkitCmd);
        } catch (Exception e) {
            getLogger().warning("Could not register command /" + name + " via CommandMap: " + e.getMessage());
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new TeleportListener(this), this);
        pm.registerEvents(securityAuditor, this);
        pm.registerEvents(new BanLoginListener(this), this);
        pm.registerEvents(new PluginHiderListener(this), this);
        pm.registerEvents(new ChatAndJoinListener(this, messageCommand), this);
        if (viaBackwardsManager != null) pm.registerEvents(viaBackwardsManager, this);
        if (wrapManager != null && wrapGui != null) pm.registerEvents(new WrapListener(this, wrapManager, wrapGui), this);
        pm.registerEvents(new WorldLocationListener(this), this);
        pm.registerEvents(new HelpAndRulesOverrideListener(this, helpCommand, rulesCommand, linkCommand, youTubeCommand, ranksCommand, invseeCommand, wrapCommand, scoreboardCommand, changeNameCommand, homeCommand, setHomeCommand, delHomeCommand, logsCommand, messageCommand, replyCommand, playerVaultCommand), this);
        pm.registerEvents(new RanksListener(this, ranksManager), this);
        pm.registerEvents(new GuiSecurityListener(this), this);
        VanishUtil.registerVanishHook(this);
    }

    public static MoonlightVanillaSurvivalCore getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public HomeManager getHomeManager() { return homeManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public SecurityAuditor getSecurityAuditor() { return securityAuditor; }
    public BedrockManager getBedrockManager() { return bedrockManager; }
    public AuditManager getAuditManager() { return auditManager; }
    public BanManager getBanManager() { return banManager; }
    public ViaBackwardsManager getViaBackwardsManager() { return viaBackwardsManager; }
    public MuteManager getMuteManager() { return muteManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public MessageCommand getMessageCommand() { return messageCommand; }
    public ProxyManager getProxyManager() { return proxyManager; }
    public WrapManager getWrapManager() { return wrapManager; }
    public WrapGui getWrapGui() { return wrapGui; }
    public WorldLocationManager getWorldLocationManager() { return worldLocationManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public HelpCommand getHelpCommand() { return helpCommand; }
    public RulesCommand getRulesCommand() { return rulesCommand; }
    public RanksManager getRanksManager() { return ranksManager; }
    public RanksGui getRanksGui() { return ranksGui; }
    public RanksCommand getRanksCommand() { return ranksCommand; }
    public PlayerVaultManager getPlayerVaultManager() { return playerVaultManager; }
    public PlayerVaultCommand getPlayerVaultCommand() { return playerVaultCommand; }
    public ReplyCommand getReplyCommand() { return replyCommand; }
}
