package me.penguinx13.wmine;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wapi.Managers.SQLiteManager;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class WMine extends JavaPlugin implements Listener, CommandExecutor {

    private ConfigManager configManager;
    private SQLiteManager sqliteManager;

    @Override
    public void onEnable() {
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
        if (sqliteManager != null) {
            sqliteManager.disconnect();
        }
    }

    public FileConfiguration getMainConfig() {
        return configManager.getConfig("config.yml");
    }

    public int getCurrencyCount(Player player) {
        return getOrCreatePlayerData(player).earnings();
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
        return getOrCreatePlayerData(player).costMultiplier();
    }

    public int getBackpackSize(Player player) {
        return getOrCreatePlayerData(player).backpack();
    }

    public int getBlocksBroken(Player player) {
        return getOrCreatePlayerData(player).blocksBroken();
    }

    public void addBrokenBlock(Player player, int reward) {
        PlayerData playerData = getOrCreatePlayerData(player);
        sqliteManager.executeUpdate(
                "UPDATE players SET name = ?, blocksBroken = ?, earnings = ? WHERE uuid = ?",
                player.getName(),
                playerData.blocksBroken() + 1,
                playerData.earnings() + reward,
                player.getUniqueId().toString()
        );
    }

    public double getPlayerParameter(String uuid, String playerName, String parameter) {
        PlayerData playerData = getOrCreatePlayerData(uuid, playerName);
        if ("backpack".equals(parameter)) {
            return playerData.backpack();
        }
        return playerData.costMultiplier();
    }

    public void setPlayerParameter(String uuid, String playerName, String parameter, double value) {
        if ("backpack".equals(parameter)) {
            int backpack = Math.max(0, (int) Math.round(value));
            sqliteManager.executeUpdate(
                    "UPDATE players SET name = ?, backpack = ? WHERE uuid = ?",
                    playerName,
                    backpack,
                    uuid
            );
            return;
        }

        sqliteManager.executeUpdate(
                "UPDATE players SET name = ?, costmultiplier = ? WHERE uuid = ?",
                playerName,
                Math.max(0, value),
                uuid
        );
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

        sqliteManager.executeUpdate(
                "UPDATE players SET earnings = 0, blocksBroken = 0, name = ? WHERE uuid = ?",
                player.getName(),
                player.getUniqueId().toString()
        );

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

    private PlayerData getOrCreatePlayerData(Player player) {
        return getOrCreatePlayerData(player.getUniqueId().toString(), player.getName());
    }

    private PlayerData getOrCreatePlayerData(String uuid, String playerName) {
        int defaultBackpack = getMainConfig().getInt("defaultValues.backpack");
        double defaultMultiplier = getMainConfig().getDouble("defaultValues.costmultiplier");

        try (PreparedStatement statement = sqliteManager.prepareStatement(
                "SELECT backpack, costmultiplier, earnings, blocksBroken FROM players WHERE uuid = ?",
                uuid
        ); ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return new PlayerData(
                        resultSet.getInt("backpack"),
                        resultSet.getDouble("costmultiplier"),
                        resultSet.getInt("earnings"),
                        resultSet.getInt("blocksBroken")
                );
            }
        } catch (SQLException exception) {
            getLogger().severe("SQL error while reading player data: " + exception.getMessage());
        }

        sqliteManager.executeUpdate(
                "INSERT INTO players (uuid, name, backpack, costmultiplier, earnings, blocksBroken) VALUES (?, ?, ?, ?, 0, 0)",
                uuid,
                playerName,
                defaultBackpack,
                defaultMultiplier
        );

        return new PlayerData(defaultBackpack, defaultMultiplier, 0, 0);
    }

    private record PlayerData(int backpack, double costMultiplier, int earnings, int blocksBroken) {
    }
}
