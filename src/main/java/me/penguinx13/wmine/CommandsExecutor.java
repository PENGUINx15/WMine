package me.penguinx13.wmine;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandsExecutor implements CommandExecutor {
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

        if (args[0].equalsIgnoreCase("claim")) {
            if (sender instanceof Player player) {
                plugin.claimCurrencyReward(player);
            } else {
                sender.sendMessage("§cЭту команду моут использовать только игроки.");
            }
        } else if (args[0].equalsIgnoreCase("info")) {
            if (sender instanceof Player player) {
                plugin.showPlayerInfo(player);
            } else {
            	sender.sendMessage("§cЭту команду моут использовать только игроки.");
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("wmine.reload")) {
            	Bukkit.getPluginManager().disablePlugin(plugin);
                Bukkit.getPluginManager().enablePlugin(plugin);
                sender.sendMessage("§aПлагин перезагружен.");
            } else {
            	sender.sendMessage("§cУ вас нету разрешения.");
            }
        } else if (args[0].equalsIgnoreCase("up")) {
            if (sender.hasPermission("wmine.up")) {
                if (args.length < 5) {
                    sender.sendMessage("§cНедостаточно аргуменов для команды 'up'.");
                    return false;
                }

                String playerName = args[1];
                String param = args[2]; // backpack або costmultiplier
                String operation = args[3]; // add, set або rem
                double amount = 0;

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

                if (param.equalsIgnoreCase("backpack") || param.equalsIgnoreCase("costmultiplier")) {
                    ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + playerName);

                    if (playerSection == null) {
                        playerSection = data.getDataConfig().createSection("players." + playerName);
                        playerSection.set("backpack", plugin.getConfig().getInt("defaultValues.backpack"));
                        playerSection.set("costmultiplier", plugin.getConfig().getDouble("defaultValues.costmultiplier"));
                    }

                    double currentAmount = playerSection.getDouble(param);

                    if (operation.equalsIgnoreCase("add")) {
                        playerSection.set(param, currentAmount + amount);
                        sender.sendMessage("§fЗначение §6" + param + "§f для игрока §6" + playerName + " §fувеличено на§6 " + amount);
                    } else if (operation.equalsIgnoreCase("set")) {
                        playerSection.set(param, amount);
                        sender.sendMessage("§fЗначение §6" + param + " §fдля игрока§6 " + playerName + "§f установлено на §6" + amount);
                    } else if (operation.equalsIgnoreCase("rem")) {
                        playerSection.set(param, currentAmount - amount);
                        sender.sendMessage("§fЗначение §6" + param + "§f для игрока §6" + playerName + " §fуменьшено на §6" + amount);
                    } else {
                    	sender.sendMessage("§cНедействительная операция, используйте: 'add', 'set' или 'rem'.");
                    }

                    data.saveDataConfig();
                } else {
                	sender.sendMessage("§cНедействительный параментр, используйте: 'backpack' или 'costmultiplier'.");
                }
            } else {
            	sender.sendMessage("§cУ вас нету разрешения.");
            }
        }

        return true;
    }
}