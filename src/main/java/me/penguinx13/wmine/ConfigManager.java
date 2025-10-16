package me.penguinx13.wmine;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;



public class ConfigManager {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    public Map<Material, Integer> blockRewards = new HashMap<>();
    public Map<Block, Material> brokenBlocks = new HashMap<>();
    public Map<UUID, Integer> playerEarnings = new HashMap<>(); // Словник для зберігання сум валюти гравців

    public Map<UUID, Integer> blocksBroken = new HashMap<>();
    public int backpack;
    public double costmultiplier;

    public int minX, minY, minZ, maxX, maxY, maxZ;
    public Location minLocation;
    public Location maxLocation;

    public boolean randomPlace;



    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void loadConfig() {
        blockRewards.clear();

        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection != null) {
            for (String blockKey : blocksSection.getKeys(false)) {
                ConfigurationSection blockData = blocksSection.getConfigurationSection(blockKey);
                if (blockData != null) {
                    String blockName = blockData.getString("block");
                    assert blockName != null;
                    Material blockType = Material.matchMaterial(blockName);
                    if (blockType != null) {
                        int reward = blockData.getInt("cost", 0);
                        blockRewards.put(blockType, reward);
                    } else {
                    	plugin.getLogger().warning("Invalid block in blocks." + blockKey + ".block: " + blockName);
                    }
                }
            }
        }

        String worldName = config.getString("location.world");
        minX = config.getInt("location.minX");
        minY = config.getInt("location.minY");
        minZ = config.getInt("location.minZ");
        maxX = config.getInt("location.maxX");
        maxY = config.getInt("location.maxY");
        maxZ = config.getInt("location.maxZ");

        assert worldName != null;
        minLocation = new Location(plugin.getServer().getWorld(worldName), minX, minY, minZ);
        maxLocation = new Location(plugin.getServer().getWorld(worldName), maxX, maxY, maxZ);

        randomPlace = config.getBoolean("randomPlace");
        backpack = config.getInt("defaultValues.backpack");
        costmultiplier = config.getDouble("defaultValues.costmultiplier");
    }
    public ConfigurationSection getBlockData(String blockKey) {
        return config.getConfigurationSection("blocks." + blockKey);
    }

    public List<String> getBlockKeys() {
        Set<String> keySet = Objects.requireNonNull(config.getConfigurationSection("blocks")).getKeys(false);
        return new ArrayList<>(keySet);
    }

}