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

        if (params.equals("money")) {
            return String.valueOf(plugin.getCurrencyCount(player));
        }
        
        if (params.equals("backpack")) {
            return String.valueOf(plugin.getBackpackSize(player));
        }
        
        if (params.equals("cm")) {
            return String.valueOf(plugin.getCostMultiplier(player));
        }
        
        if (params.equals("broken")) {
            return String.valueOf(plugin.getBlocksBroken(player));
        }
        
        if (params.startsWith("reward_")) {
        	String blockTypeName = params.substring("reward_".length());
        	Material blockType = Material.matchMaterial(blockTypeName);
            return String.valueOf(plugin.getBlockReward(player, blockType));
        }
        return null;
    }
}