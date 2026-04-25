package com.casinocore.core;

import com.casinocore.core.commands.CasinoCommand;
import com.casinocore.economy.EconomyManager;
import com.casinocore.games.blackjack.BlackjackGame;
import com.casinocore.games.blackjack.BlackjackListener;
import com.casinocore.games.commands.PlayCommand;
import com.casinocore.games.diceroll.DiceRollGame;
import com.casinocore.games.diceroll.DiceRiskListener;
import com.casinocore.games.doubleup.DoubleUpGame;
import com.casinocore.games.doubleup.DoubleUpListener;
import com.casinocore.games.highlow.HighLowGame;
import com.casinocore.games.highlow.HighLowListener;
import com.casinocore.games.GameManager;
import com.casinocore.games.horserace.HorseRaceGame;
import com.casinocore.games.horserace.HorseRaceListener;
import com.casinocore.games.impl.CoinFlipListener;
import com.casinocore.games.impl.CoinFlipGame;
import com.casinocore.games.impl.CoinFlipGuiListener;
import com.casinocore.games.impl.LotteryDrawListener;
import com.casinocore.games.impl.LotteryGame;
import com.casinocore.games.impl.LotteryPromptListener;
import com.casinocore.games.impl.WheelGame;
import com.casinocore.games.impl.WheelListener;
import com.casinocore.games.treasure.TreasureGame;
import com.casinocore.games.treasure.TreasureListener;
import com.casinocore.gui.CasinoHubListener;
import com.casinocore.gui.CustomBetListener;
import com.casinocore.games.roulette.RouletteGame;
import com.casinocore.games.roulette.RouletteListener;
import com.casinocore.games.slots.SlotMachineGame;
import com.casinocore.games.slots.SlotMachineListener;
import com.casinocore.integrations.CasinoPlaceholderExpansion;
import com.casinocore.stats.PlayerStatsManager;
import com.casinocore.utils.AntiAbuseManager;
import com.casinocore.utils.ConfigManager;
import com.casinocore.utils.CooldownManager;
import com.casinocore.utils.LocaleManager;
import com.casinocore.utils.MessageManager;
import com.casinocore.utils.ProtectionManager;
import com.casinocore.utils.UxManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class CasinoCore extends JavaPlugin implements CasinoPlugin {

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private MessageManager messageManager;
    private EconomyManager economyManager;
    private CooldownManager cooldownManager;
    private AntiAbuseManager antiAbuseManager;
    private ProtectionManager protectionManager;
    private PlayerStatsManager playerStatsManager;
    private UxManager uxManager;
    private GameManager gameManager;
    private CoinFlipGame coinFlipGame;
    private BlackjackGame blackjackGame;
    private RouletteGame rouletteGame;
    private HorseRaceGame horseRaceGame;
    private WheelGame wheelGame;
    private HighLowGame highLowGame;
    private DoubleUpGame doubleUpGame;
    private TreasureGame treasureGame;
    private PluginVariant variant;

    @Override
    public void onEnable() {
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize CasinoCore. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();
        registerEvents();
        getLogger().info("CasinoCore enabled. Variant=" + variant.name().toLowerCase() + ", games=" + gameManager.getEnabledCasinoGames().keySet());
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (cooldownManager != null) {
            cooldownManager.shutdown();
        }
        if (antiAbuseManager != null) {
            antiAbuseManager.shutdown();
        }
        if (playerStatsManager != null) {
            playerStatsManager.shutdown();
        }
        if (economyManager != null) {
            economyManager.shutdown();
        }

        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("CasinoCore disabled.");
    }

    private boolean initializeManagers() {
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            variant = resolveBuildVariant();
            localeManager = new LocaleManager(this);
            localeManager.load();

            messageManager = new MessageManager(this);
            protectionManager = new ProtectionManager(this);
            if (!protectionManager.initialize()) {
                getLogger().severe("Protection checks failed for this protected build.");
                return false;
            }

            economyManager = new EconomyManager(this);
            if (!economyManager.setupEconomy()) {
                getLogger().warning("Economy system not available.");
            }

            cooldownManager = new CooldownManager(this);
            antiAbuseManager = new AntiAbuseManager(this);
            playerStatsManager = new PlayerStatsManager(this);
            uxManager = new UxManager(this);
            gameManager = new GameManager(this);
            registerCasinoGames();
            registerIntegrations();
            return true;
        } catch (Exception e) {
            getLogger().severe("Error initializing managers: " + e.getMessage());
            return false;
        }
    }

    private PluginVariant resolveBuildVariant() {
        PluginVariant configuredVariant = PluginVariant.fromConfig(configManager.getConfig().getString("build.variant", "protected"));
        try (InputStream stream = getResource("config.yml")) {
            if (stream == null) {
                getLogger().warning("Bundled config.yml is missing. Falling back to configured build variant: " + configuredVariant.name().toLowerCase());
                return configuredVariant;
            }

            YamlConfiguration bundledConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            PluginVariant bundledVariant = PluginVariant.fromConfig(bundledConfig.getString("build.variant", configuredVariant.name()));
            if (bundledVariant != configuredVariant) {
                getLogger().warning("Config build.variant (" + configuredVariant.name().toLowerCase() + ") does not match bundled build variant (" + bundledVariant.name().toLowerCase() + "). Using bundled variant.");
            }
            return bundledVariant;
        } catch (Exception exception) {
            getLogger().warning("Failed to read bundled build variant. Falling back to configured variant: " + configuredVariant.name().toLowerCase());
            return configuredVariant;
        }
    }

    private void registerCasinoGames() {
        coinFlipGame = new CoinFlipGame(this);
        gameManager.registerCasinoGame(coinFlipGame);
        gameManager.registerCasinoGame(new SlotMachineGame(this));
        if (variant.isDemo()) {
            return;
        }

        gameManager.registerCasinoGame(new DiceRollGame(this));
        gameManager.registerCasinoGame(new LotteryGame(this));
        blackjackGame = new BlackjackGame(this);
        gameManager.registerCasinoGame(blackjackGame);
        rouletteGame = new RouletteGame(this);
        gameManager.registerCasinoGame(rouletteGame);
        horseRaceGame = new HorseRaceGame(this);
        gameManager.registerCasinoGame(horseRaceGame);
        wheelGame = new WheelGame(this);
        gameManager.registerCasinoGame(wheelGame);
        highLowGame = new HighLowGame(this);
        gameManager.registerCasinoGame(highLowGame);
        doubleUpGame = new DoubleUpGame(this);
        gameManager.registerCasinoGame(doubleUpGame);
        treasureGame = new TreasureGame(this);
        gameManager.registerCasinoGame(treasureGame);
    }

    private void registerCommands() {
        getCommand("casino").setExecutor(new CasinoCommand(this));
        getCommand("play").setExecutor(new PlayCommand(this));
    }

    private void registerIntegrations() {
        if (variant.isDemo()) {
            return;
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CasinoPlaceholderExpansion(this).register();
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new CasinoHubListener(), this);
        getServer().getPluginManager().registerEvents(new CustomBetListener(this), this);
        getServer().getPluginManager().registerEvents(new SlotMachineListener(), this);
        if (!variant.isDemo()) {
            getServer().getPluginManager().registerEvents(new DiceRiskListener(), this);
            getServer().getPluginManager().registerEvents(new LotteryPromptListener(this), this);
            getServer().getPluginManager().registerEvents(new LotteryDrawListener(this), this);
        }
        if (coinFlipGame != null) {
            getServer().getPluginManager().registerEvents(new CoinFlipListener(coinFlipGame), this);
            getServer().getPluginManager().registerEvents(new CoinFlipGuiListener(), this);
        }
        if (blackjackGame != null) {
            getServer().getPluginManager().registerEvents(new BlackjackListener(blackjackGame), this);
        }
        if (rouletteGame != null) {
            getServer().getPluginManager().registerEvents(new RouletteListener(rouletteGame), this);
        }
        if (horseRaceGame != null) {
            getServer().getPluginManager().registerEvents(new HorseRaceListener(horseRaceGame), this);
        }
        if (wheelGame != null) {
            getServer().getPluginManager().registerEvents(new WheelListener(wheelGame), this);
        }
        if (highLowGame != null) {
            getServer().getPluginManager().registerEvents(new HighLowListener(highLowGame), this);
        }
        if (doubleUpGame != null) {
            getServer().getPluginManager().registerEvents(new DoubleUpListener(doubleUpGame), this);
        }
        if (treasureGame != null) {
            getServer().getPluginManager().registerEvents(new TreasureListener(treasureGame), this);
        }
    }

    public void reloadPlugin() {
        configManager.reloadConfig();
        messageManager.reload();
        gameManager.reloadGames();
    }

    @Override
    public JavaPlugin getPlugin() {
        return this;
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public MessageManager getMessageManager() {
        return messageManager;
    }

    @Override
    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    @Override
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    @Override
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    @Override
    public AntiAbuseManager getAntiAbuseManager() {
        return antiAbuseManager;
    }

    @Override
    public PlayerStatsManager getPlayerStatsManager() {
        return playerStatsManager;
    }

    @Override
    public UxManager getUxManager() {
        return uxManager;
    }

    @Override
    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    @Override
    public GameManager getGameManager() {
        return gameManager;
    }

    @Override
    public PluginVariant getVariant() {
        return variant;
    }
}
