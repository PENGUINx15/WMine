package me.penguinx13.wmine;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandsExecutor implements CommandExecutor {
    private static final String PLAYERS_PATH = "players.";

    private final WMine plugin;
    private final DataConfigManager data;

    public CommandsExecutor(WMine plugin, DataConfigManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cНедостаточно аргументов.");
            return false;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "claim" -> handleClaim(sender);
            case "info" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            case "up" -> handleUpgrade(sender, args);
            default -> true;
        };
    }

    private boolean handleClaim(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.claimCurrencyReward(player);
        } else {
            sender.sendMessage("§cЭту команду моут использовать только игроки.");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.showPlayerInfo(player);
        } else {
            sender.sendMessage("§cЭту команду моут использовать только игроки.");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("wmine.reload")) {
            sender.sendMessage("§cУ вас нету разрешения.");
            return true;
        }

        Bukkit.getPluginManager().disablePlugin(plugin);
        Bukkit.getPluginManager().enablePlugin(plugin);
        sender.sendMessage("§aПлагин перезагружен.");
        return true;
    }

    private boolean handleUpgrade(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wmine.up")) {
            sender.sendMessage("§cУ вас нету разрешения.");
            return true;
        }

        if (args.length < 5) {
            sender.sendMessage("§cНедостаточно аргуменов для команды 'up'.");
            return false;
        }

        String playerName = args[1];
        String param = args[2].toLowerCase();
        String operation = args[3].toLowerCase();

        double amount;
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеправельный формат.");
            return false;
        }

        if (!playerName.matches("[A-Za-z0-9_]+")) {
            sender.sendMessage("§cНеправельное имя ирока.");
            return false;
        }

        if (!param.equals("backpack") && !param.equals("costmultiplier")) {
            sender.sendMessage("§cНедействительный параментр, используйте: 'backpack' или 'costmultiplier'.");
            return true;
        }

        ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection(PLAYERS_PATH + playerName);
        if (playerSection == null) {
            playerSection = data.getDataConfig().createSection(PLAYERS_PATH + playerName);
            playerSection.set("backpack", plugin.getConfig().getInt("defaultValues.backpack"));
            playerSection.set("costmultiplier", plugin.getConfig().getDouble("defaultValues.costmultiplier"));
        }

        double currentAmount = playerSection.getDouble(param);
        switch (operation) {
            case "add" -> {
                playerSection.set(param, currentAmount + amount);
                sender.sendMessage("§fЗначение §6" + param + "§f для игрока §6" + playerName + " §fувеличено на§6 " + amount);
            }
            case "set" -> {
                playerSection.set(param, amount);
                sender.sendMessage("§fЗначение §6" + param + " §fдля игрока§6 " + playerName + "§f установлено на §6" + amount);
            }
            case "rem" -> {
                playerSection.set(param, currentAmount - amount);
                sender.sendMessage("§fЗначение §6" + param + "§f для игрока §6" + playerName + " §fуменьшено на §6" + amount);
            }
            default -> {
                sender.sendMessage("§cНедействительная операция, используйте: 'add', 'set' или 'rem'.");
                return true;
            }
        }

        data.saveDataConfig();
        return true;
    }
}
