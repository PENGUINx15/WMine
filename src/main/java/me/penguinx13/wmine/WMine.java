package me.penguinx13.wmine;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

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

        BlockBreakListener blockBreakListener = new BlockBreakListener(config, this, data);  // Створіть новий об'єкт
        getServer().getPluginManager().registerEvents(blockBreakListener, this); // Зареєструйте події

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
        if (data != null && config != null) {
            ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + player.getName());

            if (playerSection != null) {
                double costMultiplier = playerSection.getDouble("costmultiplier", config.costmultiplier);
                Integer reward = config.blockRewards.get(blockType);


                if (reward != null) {
                    return reward * costMultiplier;
                }
            }
        }
        assert config != null;
        return config.blockRewards.get(blockType);
    }

    public double getCostMultiplier(Player player) {
        ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + player.getName());
        return playerSection != null ? playerSection.getDouble("costmultiplier") : getConfig().getDouble("defaultValues.costmultiplier");
    }


    public int getBackpackSize(Player player) {
        ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + player.getName());
        return playerSection != null ? playerSection.getInt("backpack") : getConfig().getInt("defaultValues.backpack");
    }

    public int getBlocksBroken(Player player) {
        UUID playerId = player.getUniqueId();
        return config.blocksBroken.getOrDefault(playerId, 0);
    }



    public void claimCurrencyReward(Player player) {
        UUID playerId = player.getUniqueId();
        if (config != null) {
            int currencyEarned = config.playerEarnings.getOrDefault(playerId, 0);

            Economy economy = Objects.requireNonNull(getServer().getServicesManager().getRegistration(Economy.class)).getProvider();
            economy.depositPlayer(player, currencyEarned);

            player.sendMessage("§fВы получили зарплату §6" + currencyEarned + "$");
            config.playerEarnings.remove(playerId);

            config.blocksBroken.remove(playerId);
        } else {
            getLogger().info("config = null");
        }
    }


    public void showPlayerInfo(Player player) {
        UUID playerId = player.getUniqueId();
        int currencyEarned = config.playerEarnings.getOrDefault(playerId, 0);
        int blocksBrokenByPlayer = config.blocksBroken.getOrDefault(playerId, 0);

        if (config != null) {
            ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + player.getName());

            if (playerSection != null) {
                int backpackSize = playerSection.getInt("backpack", config.backpack);
                player.sendMessage("§f---------------------[§6Шахта§f]---------------------");
                player.sendMessage("§fОжидаемая зарплата: §6" + currencyEarned + "$");
                player.sendMessage("§fРюкзак: §e" + blocksBrokenByPlayer + "§f/§6" + backpackSize);
                player.sendMessage("§f§n-------------------------------------------------");
            }
        } else {
            getLogger().info("config = null");
        }
    }

}