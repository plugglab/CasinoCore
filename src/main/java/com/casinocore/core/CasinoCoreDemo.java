package com.casinocore.core;

import com.casinocore.core.commands.CasinoCommand;
import com.casinocore.economy.EconomyManager;
import com.casinocore.games.GameManager;
import com.casinocore.games.commands.DemoPlayCommand;
import com.casinocore.games.impl.CoinFlipGame;
import com.casinocore.games.impl.CoinFlipGuiListener;
import com.casinocore.games.impl.CoinFlipListener;
import com.casinocore.games.slots.SlotMachineGame;
import com.casinocore.games.slots.SlotMachineListener;
import com.casinocore.gui.CasinoHubListener;
import com.casinocore.gui.CustomBetListener;
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

public final class CasinoCoreDemo extends JavaPlugin implements CasinoPlugin {

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private MessageManager messageManager;
    private EconomyManager economyManager;
    private CooldownManager cooldownManager;
    private AntiAbuseManager antiAbuseManager;
    private PlayerStatsManager playerStatsManager;
    private UxManager uxManager;
    private GameManager gameManager;
    private PluginVariant variant;

    @Override
    public void onEnable() {
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize CasinoCore demo. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();
        registerEvents();
        getLogger().info("CasinoCore demo enabled. Games=" + gameManager.getEnabledCasinoGames().keySet());
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
        getLogger().info("CasinoCore demo disabled.");
    }

    private boolean initializeManagers() {
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            variant = resolveBuildVariant();
            localeManager = new LocaleManager(this);
            localeManager.load();
            messageManager = new MessageManager(this);
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
            return true;
        } catch (Exception e) {
            getLogger().severe("Error initializing demo managers: " + e.getMessage());
            return false;
        }
    }

    private PluginVariant resolveBuildVariant() {
        PluginVariant configuredVariant = PluginVariant.fromConfig(configManager.getConfig().getString("build.variant", "demo"));
        try (InputStream stream = getResource("config.yml")) {
            if (stream == null) {
                return configuredVariant;
            }

            YamlConfiguration bundledConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return PluginVariant.fromConfig(bundledConfig.getString("build.variant", configuredVariant.name()));
        } catch (Exception exception) {
            return configuredVariant;
        }
    }

    private void registerCasinoGames() {
        CoinFlipGame coinFlipGame = new CoinFlipGame(this);
        gameManager.registerCasinoGame(coinFlipGame);
        gameManager.registerCasinoGame(new SlotMachineGame(this));
    }

    private void registerCommands() {
        getCommand("casino").setExecutor(new CasinoCommand(this));
        getCommand("play").setExecutor(new DemoPlayCommand(this));
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new CasinoHubListener(), this);
        getServer().getPluginManager().registerEvents(new CustomBetListener(this), this);
        getServer().getPluginManager().registerEvents(new SlotMachineListener(), this);

        CoinFlipGame coinFlipGame = (CoinFlipGame) gameManager.getCasinoGameDirect("coinflip");
        if (coinFlipGame != null) {
            getServer().getPluginManager().registerEvents(new CoinFlipListener(coinFlipGame), this);
            getServer().getPluginManager().registerEvents(new CoinFlipGuiListener(), this);
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
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    @Override
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    @Override
    public GameManager getGameManager() {
        return gameManager;
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
    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    @Override
    public ProtectionManager getProtectionManager() {
        return null;
    }

    @Override
    public PluginVariant getVariant() {
        return variant;
    }
}
