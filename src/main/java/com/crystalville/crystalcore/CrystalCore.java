package com.crystalville.crystalcore;

import com.crystalville.crystalcore.commands.BuyCommand;
import com.crystalville.crystalcore.commands.ClaimCommand;
import com.crystalville.crystalcore.commands.DraftCommand;
import com.crystalville.crystalcore.commands.EditHudCommand;
import com.crystalville.crystalcore.commands.HoleFillerCommand;
import com.crystalville.crystalcore.commands.HudCommand;
import com.crystalville.crystalcore.commands.InspectCommand;
import com.crystalville.crystalcore.commands.InventoryCommand;
import com.crystalville.crystalcore.commands.LogoCommand;
import com.crystalville.crystalcore.commands.PayCommand;
import com.crystalville.crystalcore.commands.RankCommand;
import com.crystalville.crystalcore.commands.RoleCommand;
import com.crystalville.crystalcore.commands.SellCommand;
import com.crystalville.crystalcore.commands.ShopCommand;
import com.crystalville.crystalcore.listeners.AntiTheftListener;
import com.crystalville.crystalcore.listeners.CrystalListener;
import com.crystalville.crystalcore.listeners.HoleFillerListener;
import com.crystalville.crystalcore.listeners.HudListener;
import com.crystalville.crystalcore.managers.ChestLogManager;
import com.crystalville.crystalcore.managers.CrystalRenameTask;
import com.crystalville.crystalcore.managers.HoleFillerManager;
import com.crystalville.crystalcore.managers.HudManager;
import com.crystalville.crystalcore.managers.InspectorManager;
import com.crystalville.crystalcore.managers.MailboxManager;
import com.crystalville.crystalcore.managers.RankManager;
import com.crystalville.crystalcore.managers.ShopManager;
import com.crystalville.crystalcore.managers.StatsManager;
import com.crystalville.crystalcore.util.CrystalItemUtil;
import com.crystalville.crystalcore.util.HudItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrystalCore extends JavaPlugin {

    private RankManager rankManager;
    private ShopManager shopManager;
    private MailboxManager mailboxManager;
    private ChestLogManager chestLogManager;
    private InspectorManager inspectorManager;
    private HoleFillerManager holeFillerManager;
    private HudManager hudManager;
    private StatsManager statsManager;
    private HudListener hudListener;
    private Material payCurrency;
    private Material buyCurrency;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String payMatName = getConfig().getString("pay-currency-item", "AMETHYST_SHARD");
        String buyMatName = getConfig().getString("buy-currency-item", "AMETHYST_SHARD");

        Material parsedPay = Material.matchMaterial(payMatName);
        Material parsedBuy = Material.matchMaterial(buyMatName);

        this.payCurrency = parsedPay != null ? parsedPay : Material.AMETHYST_SHARD;
        this.buyCurrency = parsedBuy != null ? parsedBuy : Material.AMETHYST_SHARD;

        CrystalItemUtil.init(this);
        HudItemUtil.init(this);

        this.rankManager = new RankManager(this);
        this.rankManager.loadRanks();

        this.shopManager = new ShopManager(this);
        this.shopManager.loadShop();

        this.mailboxManager = new MailboxManager(this);
        this.mailboxManager.load();

        this.chestLogManager = new ChestLogManager(this);
        this.chestLogManager.load();

        this.inspectorManager = new InspectorManager();
        this.holeFillerManager = new HoleFillerManager();

        this.hudManager = new HudManager(this);
        this.hudManager.load();

        this.statsManager = new StatsManager(this);
        this.statsManager.load();

        this.hudListener = new HudListener(this, hudManager, rankManager, statsManager);

        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("buy").setExecutor(new BuyCommand(shopManager, rankManager));
        getCommand("sell").setExecutor(new SellCommand(shopManager));
        getCommand("rank").setExecutor(new RankCommand(this, rankManager));
        getCommand("role").setExecutor(new RoleCommand(rankManager));
        getCommand("logo").setExecutor(new LogoCommand());
        getCommand("claim").setExecutor(new ClaimCommand(mailboxManager));
        getCommand("i").setExecutor(new InspectCommand(inspectorManager));
        getCommand("holefiller").setExecutor(new HoleFillerCommand(holeFillerManager));
        getCommand("inventory").setExecutor(new InventoryCommand());
        getCommand("shop").setExecutor(new ShopCommand(shopManager, rankManager));
        getCommand("draft").setExecutor(new DraftCommand(mailboxManager));
        getCommand("hud").setExecutor(new HudCommand(hudManager, hudListener));
        getCommand("edit").setExecutor(new EditHudCommand(hudManager, hudListener));

        getServer().getPluginManager().registerEvents(new CrystalListener(rankManager, mailboxManager), this);
        getServer().getPluginManager().registerEvents(
                new AntiTheftListener(chestLogManager, inspectorManager), this);
        getServer().getPluginManager().registerEvents(new HoleFillerListener(holeFillerManager), this);
        getServer().getPluginManager().registerEvents(hudListener, this);

        if (getConfig().getBoolean("rename-amethyst-to-crystal", true)) {
            long interval = getConfig().getLong("rename-scan-interval-ticks", 100L);
            new CrystalRenameTask().runTaskTimer(this, 40L, interval);
        }

        // Refresh every online player's HUD once a second (playtime tick, live Crystal count).
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                if (hudManager.isEnabled(player.getUniqueId())) {
                    hudListener.refresh(player);
                }
            }
        }, 40L, 20L);

        getLogger().info("CrystalCore has been enabled. Currency: Crystal (Amethyst Shard) | Shop items loaded: "
                + shopManager.getAllPrices().size());
    }

    @Override
    public void onDisable() {
        if (rankManager != null) {
            rankManager.saveRanks();
        }
        if (mailboxManager != null) {
            mailboxManager.save();
        }
        if (chestLogManager != null) {
            chestLogManager.save();
        }
        if (hudManager != null) {
            hudManager.save();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        getLogger().info("CrystalCore has been disabled.");
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

    public Material getPayCurrency() {
        return payCurrency;
    }

    public Material getBuyCurrency() {
        return buyCurrency;
    }
  }
