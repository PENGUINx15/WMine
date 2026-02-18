package me.penguinx13.wmine;

import me.penguinx13.wapi.Managers.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class WMine extends JavaPlugin implements Listener, CommandExecutor {
    private static final String PLAYERS_PATH = "players.";

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.registerConfig("config.yml");
        configManager.registerConfig("data.yml");

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this, configManager), this);
        Objects.requireNonNull(getCommand("wmine")).setExecutor(new CommandsExecutor(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
            getLogger().info("Плейсхолдеры WMine зарегистрированы успешно!");
        }
    }

    public FileConfiguration getMainConfig() {
        return configManager.getConfig("config.yml");
    }

    public FileConfiguration getDataConfig() {
        return configManager.getConfig("data.yml");
    }

    public void saveDataConfig() {
        try {
            getDataConfig().save(new File(getDataFolder(), "data.yml"));
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Не удалось сохранить data.yml", exception);
        }
    }

    public int getCurrencyCount(Player player) {
        return getPlayerSection(player).getInt("earnings", 0);
    }

    public double getBlockReward(Player player, Material blockType) {
        if (blockType == null) {
            return 0;
        }

        ConfigurationSection blocksSection = getMainConfig().getConfigurationSection("blocks");
        if (blocksSection == null) {
            return 0;
        }

        Integer reward = null;
        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
            if (blockSection == null) {
                continue;
            }

            Material configuredMaterial = Material.matchMaterial(blockSection.getString("block", ""));
            if (configuredMaterial == blockType) {
                reward = blockSection.getInt("cost", 0);
                break;
            }
        }

        if (reward == null) {
            return 0;
        }

        return reward * getCostMultiplier(player);
    }

    public double getCostMultiplier(Player player) {
        ConfigurationSection playerSection = getPlayerSection(player);
        return playerSection.getDouble("costmultiplier", getMainConfig().getDouble("defaultValues.costmultiplier"));
    }

    public int getBackpackSize(Player player) {
        ConfigurationSection playerSection = getPlayerSection(player);
        return playerSection.getInt("backpack", getMainConfig().getInt("defaultValues.backpack"));
    }

    public int getBlocksBroken(Player player) {
        return getPlayerSection(player).getInt("blocksBroken", 0);
    }

    public void claimCurrencyReward(Player player) {
        int currencyEarned = getCurrencyCount(player);

        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            getLogger().warning("Vault Economy provider not found.");
            player.sendMessage("§cЭкономика недоступна.");
            return;
        }

        Economy economy = registration.getProvider();
        economy.depositPlayer(player, currencyEarned);

        ConfigurationSection playerSection = getPlayerSection(player);
        playerSection.set("earnings", 0);
        playerSection.set("blocksBroken", 0);
        saveDataConfig();

        player.sendMessage("§fВы получили зарплату §6" + currencyEarned + "$");
    }

    public void showPlayerInfo(Player player) {
        int currencyEarned = getCurrencyCount(player);
        int blocksBrokenByPlayer = getBlocksBroken(player);
        int backpackSize = getBackpackSize(player);

        player.sendMessage("§f---------------------[§6Шахта§f]---------------------");
        player.sendMessage("§fОжидаемая зарплата: §6" + currencyEarned + "$");
        player.sendMessage("§fРюкзак: §e" + blocksBrokenByPlayer + "§f/§6" + backpackSize);
        player.sendMessage("§f§n-------------------------------------------------");
    }

    private ConfigurationSection getPlayerSection(Player player) {
        String playerPath = PLAYERS_PATH + player.getName();
        ConfigurationSection playerSection = getDataConfig().getConfigurationSection(playerPath);
        if (playerSection == null) {
            playerSection = getDataConfig().createSection(playerPath);
            playerSection.set("backpack", getMainConfig().getInt("defaultValues.backpack"));
            playerSection.set("costmultiplier", getMainConfig().getDouble("defaultValues.costmultiplier"));
            playerSection.set("earnings", 0);
            playerSection.set("blocksBroken", 0);
            saveDataConfig();
        }

        return playerSection;
    }
}
