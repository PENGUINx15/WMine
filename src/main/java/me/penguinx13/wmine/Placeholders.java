package me.penguinx13.wmine;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Placeholders extends PlaceholderExpansion {

    private final WMine plugin;

    public Placeholders(WMine plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        // Ідентифікатор плейсхолдера - це текст, який буде використовуватися у синтаксисі плейсхолдера.
        return "wmine";
    }

    @Override
    public String getAuthor() {
        // Поверніть ім'я або авторів плейсхолдера.
        return "penguin";
    }

    @Override
    public String getVersion() {
        // Поверніть версію вашого плейсхолдера.
        return "1.0";
    }

    @Override
    public boolean persist() {
        // Повертаємо true, щоб зберегти значення плейсхолдера в конфігурації PlaceholderAPI
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        return switch (params) {
            case "money" -> String.valueOf(plugin.getCurrencyCount(player));
            case "backpack" -> String.valueOf(plugin.getBackpackSize(player));
            case "cm" -> String.valueOf(plugin.getCostMultiplier(player));
            case "broken" -> String.valueOf(plugin.getBlocksBroken(player));
            default -> getRewardPlaceholder(player, params);
        };
    }

    private String getRewardPlaceholder(Player player, String params) {
        if (!params.startsWith("reward_")) {
            return null;
        }

        String blockTypeName = params.substring("reward_".length());
        Material blockType = Material.matchMaterial(blockTypeName);
        return String.valueOf(plugin.getBlockReward(player, blockType));
    }
}