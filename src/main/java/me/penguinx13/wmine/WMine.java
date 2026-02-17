package me.penguinx13.wmine;

import java.util.Objects;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class WMine extends JavaPlugin implements Listener, CommandExecutor {
    private ConfigManager config;
    private DataConfigManager data;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = new ConfigManager(this);
        config.loadConfig();

        data = new DataConfigManager(this);
        data.setupDataConfig();

        getServer().getPluginManager().registerEvents(new BlockBreakListener(config, this, data), this);
        Objects.requireNonNull(getCommand("wmine")).setExecutor(new CommandsExecutor(this, data));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
            getLogger().info("Плейсхолдеры WMine зареестрированы успешно!");
        }
    }

    public int getCurrencyCount(Player player) {
        return config.playerEarnings.getOrDefault(player.getUniqueId(), 0);
    }

    public double getBlockReward(Player player, Material blockType) {
        if (blockType == null) {
            return 0;
        }

        Integer reward = config.blockRewards.get(blockType);
        if (reward == null) {
            return 0;
        }

        return reward * getCostMultiplier(player);
    }

    public double getCostMultiplier(Player player) {
        ConfigurationSection playerSection = getPlayerSection(player);
        return playerSection != null
                ? playerSection.getDouble("costmultiplier", config.costmultiplier)
                : config.costmultiplier;
    }

    public int getBackpackSize(Player player) {
        ConfigurationSection playerSection = getPlayerSection(player);
        return playerSection != null
                ? playerSection.getInt("backpack", config.backpack)
                : config.backpack;
    }

    public int getBlocksBroken(Player player) {
        UUID playerId = player.getUniqueId();
        return config.blocksBroken.getOrDefault(playerId, 0);
    }

    public void claimCurrencyReward(Player player) {
        UUID playerId = player.getUniqueId();
        int currencyEarned = config.playerEarnings.getOrDefault(playerId, 0);

        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            getLogger().warning("Vault Economy provider not found.");
            player.sendMessage("§cЭкономика недоступна.");
            return;
        }

        Economy economy = registration.getProvider();
        economy.depositPlayer(player, currencyEarned);

        player.sendMessage("§fВы получили зарплату §6" + currencyEarned + "$");
        config.playerEarnings.remove(playerId);
        config.blocksBroken.remove(playerId);
    }

    public void showPlayerInfo(Player player) {
        UUID playerId = player.getUniqueId();
        int currencyEarned = config.playerEarnings.getOrDefault(playerId, 0);
        int blocksBrokenByPlayer = config.blocksBroken.getOrDefault(playerId, 0);
        int backpackSize = getBackpackSize(player);

        player.sendMessage("§f---------------------[§6Шахта§f]---------------------");
        player.sendMessage("§fОжидаемая зарплата: §6" + currencyEarned + "$");
        player.sendMessage("§fРюкзак: §e" + blocksBrokenByPlayer + "§f/§6" + backpackSize);
        player.sendMessage("§f§n-------------------------------------------------");
    }

    private ConfigurationSection getPlayerSection(Player player) {
        return data.getDataConfig().getConfigurationSection("players." + player.getName());
    }
}
