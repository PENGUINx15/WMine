package me.penguinx13.wmine;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandsExecutor implements CommandExecutor {

    private static final String MSG_ONLY_PLAYER = "§cЭту команду моут использовать только игроки.";

    private final WMine plugin;

    public CommandsExecutor(WMine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cНедостаточно аргументов.");
            return false;
        }

        return switch (args[0].toLowerCase()) {
            case "claim" -> handleClaim(sender);
            case "info" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            case "up" -> handleUpgrade(sender, args);
            default -> true;
        };
    }

    private boolean handleClaim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG_ONLY_PLAYER);
            return true;
        }

        plugin.claimCurrencyReward(player);
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG_ONLY_PLAYER);
            return true;
        }

        plugin.showPlayerInfo(player);
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
        String parameter = args[2].toLowerCase();
        String operation = args[3].toLowerCase();

        if (!playerName.matches("[A-Za-z0-9_]+")) {
            sender.sendMessage("§cНеправельное имя ирока.");
            return false;
        }

        if (!parameter.equals("backpack") && !parameter.equals("costmultiplier")) {
            sender.sendMessage("§cНедействительный параментр, используйте: 'backpack' или 'costmultiplier'.");
            return true;
        }

        Double amount = parseAmount(sender, args[4]);
        if (amount == null) {
            return false;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        String uuid = offlinePlayer.getUniqueId().toString();

        double currentValue = plugin.getPlayerParameter(uuid, playerName, parameter);
        double newValue = calculateValue(currentValue, amount, operation);

        if (Double.isNaN(newValue)) {
            sender.sendMessage("§cНедействительная операция, используйте: 'add', 'set' или 'rem'.");
            return true;
        }

        sendUpgradeMessage(sender, parameter, playerName, amount, operation);
        plugin.setPlayerParameter(uuid, playerName, parameter, newValue);
        return true;
    }

    private Double parseAmount(CommandSender sender, String rawAmount) {
        try {
            return Double.parseDouble(rawAmount);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§cНеправельный формат.");
            return null;
        }
    }

    private double calculateValue(double currentValue, double amount, String operation) {
        return switch (operation) {
            case "add" -> currentValue + amount;
            case "set" -> amount;
            case "rem" -> currentValue - amount;
            default -> Double.NaN;
        };
    }

    private void sendUpgradeMessage(CommandSender sender, String parameter, String playerName, double amount, String operation) {
        switch (operation) {
            case "add" -> sender.sendMessage("§fЗначение §6" + parameter + "§f для игрока §6" + playerName + " §fувеличено на§6 " + amount);
            case "set" -> sender.sendMessage("§fЗначение §6" + parameter + " §fдля игрока§6 " + playerName + "§f установлено на §6" + amount);
            case "rem" -> sender.sendMessage("§fЗначение §6" + parameter + "§f для игрока §6" + playerName + " §fуменьшено на §6" + amount);
            default -> {
            }
        }
    }
}
