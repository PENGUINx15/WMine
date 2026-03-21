package me.penguinx13.wmine;

import me.penguinx13.wapi.managers.ConfigManager;
import me.penguinx13.wapi.managers.SQLiteManager;
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

import java.util.Objects;

public class WMine extends JavaPlugin implements Listener, CommandExecutor {

    private static WMine instance;

    private ConfigManager configManager;
    private SQLiteManager sqliteManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.registerConfig("config.yml");

        sqliteManager = new SQLiteManager(this, "players.db");
        sqliteManager.connect();
        sqliteManager.executeUpdate(
                "CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "backpack INTEGER NOT NULL," +
                        "costmultiplier REAL NOT NULL," +
                        "earnings INTEGER NOT NULL DEFAULT 0," +
                        "blocksBroken INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this, configManager), this);
        Objects.requireNonNull(getCommand("wmine")).setExecutor(new CommandsExecutor(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
            getLogger().info("Плейсхолдеры WMine зарегистрированы успешно!");
        }
    }

    @Override
    public void onDisable() {
        instance = null;

        if (sqliteManager != null) {
            sqliteManager.disconnect();
        }
    }

    public FileConfiguration getMainConfig() {
        return configManager.getConfig("config.yml");
    }

    public PlayerData getPlayerData(Player player) {
        return PlayerData.getOrCreate(player);
    }

    public double getBlockReward(Player player, Material blockType) {
        int reward = getBaseBlockReward(blockType);
        if (reward <= 0) {
            return 0;
        }

        return reward * getPlayerData(player).costMultiplier();
    }

    public int getBaseBlockReward(Material blockType) {
        if (blockType == null) {
            return -1;
        }

        ConfigurationSection blocksSection = getMainConfig().getConfigurationSection("blocks");
        if (blocksSection == null) {
            return -1;
        }

        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
            if (blockSection == null) {
                continue;
            }

            Material configuredMaterial = Material.matchMaterial(blockSection.getString("block", ""));
            if (configuredMaterial == blockType) {
                return blockSection.getInt("cost", -1);
            }
        }

        return -1;
    }

    public void addBrokenBlock(Player player, int reward) {
        PlayerData.addBrokenBlock(player, reward);
    }

    public double getPlayerParameter(String uuid, String playerName, String parameter) {
        return PlayerData.getParameter(uuid, playerName, parameter);
    }

    public void setPlayerParameter(String uuid, String playerName, String parameter, double value) {
        PlayerData.setParameter(uuid, playerName, parameter, value);
    }

    public void claimCurrencyReward(Player player) {
        PlayerData playerData = getPlayerData(player);

        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            getLogger().warning("Vault Economy provider not found.");
            player.sendMessage("§cЭкономика недоступна.");
            return;
        }

        Economy economy = registration.getProvider();
        economy.depositPlayer(player, playerData.earnings());

        PlayerData.resetClaimData(player);

        player.sendMessage("§fВы получили зарплату §6" + playerData.earnings() + "$");
    }

    public void showPlayerInfo(Player player) {
        PlayerData playerData = getPlayerData(player);

        player.sendMessage("§f---------------------[§6Шахта§f]---------------------");
        player.sendMessage("§fОжидаемая зарплата: §6" + playerData.earnings() + "$");
        player.sendMessage("§fРюкзак: §e" + playerData.blocksBroken() + "§f/§6" + playerData.backpack());
        player.sendMessage("§f§n-------------------------------------------------");
    }


    public static WMine getInstance() {
        return instance;
    }

    SQLiteManager getSqliteManager() {
        return sqliteManager;
    }
}

