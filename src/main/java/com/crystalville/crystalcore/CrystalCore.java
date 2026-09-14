package com.crystalville.crystalcore;

import com.crystalville.crystalcore.commands.AdminCommand;
import com.crystalville.crystalcore.commands.AfkCommand;
import com.crystalville.crystalcore.commands.BalanceCommand;
import com.crystalville.crystalcore.commands.CrystalCommand;
import com.crystalville.crystalcore.commands.HoleCommand;
import com.crystalville.crystalcore.commands.InspectCommand;
import com.crystalville.crystalcore.commands.LinkCommand;
import com.crystalville.crystalcore.commands.MailCommand;
import com.crystalville.crystalcore.commands.PayCommand;
import com.crystalville.crystalcore.commands.RankCommand;
import com.crystalville.crystalcore.commands.ShopCommand;
import com.crystalville.crystalcore.commands.StatsCommand;
import com.crystalville.crystalcore.commands.WebLinkCommand;
import com.crystalville.crystalcore.commands.WithdrawCommand;
import com.crystalville.crystalcore.listeners.AfkListener;
import com.crystalville.crystalcore.listeners.ChestLogListener;
import com.crystalville.crystalcore.listeners.HudListener;
import com.crystalville.crystalcore.listeners.InspectorListener;
import com.crystalville.crystalcore.listeners.RankListener;
import com.crystalville.crystalcore.listeners.StatsListener;
import com.crystalville.crystalcore.managers.ChestLogManager;
import com.crystalville.crystalcore.managers.HoleFillerManager;
import com.crystalville.crystalcore.managers.HudManager;
import com.crystalville.crystalcore.managers.InspectorManager;
import com.crystalville.crystalcore.managers.MailboxManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.managers.ShopManager;
import com.crystalville.crystalcore.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrystalCore extends JavaPlugin {

    private static CrystalCore instance;

    private RankManager rankManager;
    private ShopManager shopManager;
    private MailboxManager mailboxManager;
    private ChestLogManager chestLogManager;
    private InspectorManager inspectorManager;
    private HoleFillerManager holeFillerManager;
    private HudManager hudManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        rankManager = new RankManager(this);
        shopManager = new ShopManager(this);
        mailboxManager = new MailboxManager(this);
        chestLogManager = new ChestLogManager(this);
        inspectorManager = new InspectorManager(this);
        holeFillerManager = new HoleFillerManager(this);
        hudManager = new HudManager(this);
        statsManager = new StatsManager(this);

        getCommand("crystal").setExecutor(new CrystalCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("withdraw").setExecutor(new WithdrawCommand(this));
        getCommand("mail").setExecutor(new MailCommand(this));
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("admin").setExecutor(new AdminCommand(this));
        getCommand("afk").setExecutor(new AfkCommand(this));
        getCommand("inspect").setExecutor(new InspectCommand(this));
        getCommand("hole").setExecutor(new HoleCommand(this));
        getCommand("link").setExecutor(new LinkCommand(this));
        getCommand("cvlink").setExecutor(new WebLinkCommand(this));

        Bukkit.getPluginManager().registerEvents(new RankListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AfkListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChestLogListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InspectorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HudListener(this), this);

        statsManager.start();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                hudManager.update(player);
            }
        }, 20L, 20L);

        getLogger().info("CrystalCore enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            statsManager.stop();
        }

        getLogger().info("CrystalCore disabled.");
    }

    public static CrystalCore getInstance() {
        return instance;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public MailboxManager getMailboxManager() {
        return mailboxManager;
    }

    public ChestLogManager getChestLogManager() {
        return chestLogManager;
    }

    public InspectorManager getInspectorManager() {
        return inspectorManager;
    }

    public HoleFillerManager getHoleFillerManager() {
        return holeFillerManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }
}
