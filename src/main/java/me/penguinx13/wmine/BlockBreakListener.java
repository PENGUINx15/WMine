package me.penguinx13.wmine;

import me.penguinx13.wapi.managers.ConfigManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BlockBreakListener implements Listener {

    private final Plugin plugin;
    private final WMine wMine;
    private final FileConfiguration config;
    private final Map<Block, Material> brokenBlocks = new HashMap<>();

    public BlockBreakListener(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.wMine = (WMine) plugin;
        this.config = configManager.getConfig("config.yml");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!isInMine(block.getLocation())) {
            return;
        }

        Material material = block.getType();
        int baseReward = wMine.getBaseBlockReward(material);
        if (baseReward <= 0) {
            event.setCancelled(true);
            return;
        }

        if (isBackpackFull(player)) {
            event.setCancelled(true);
            player.sendMessage("§cВаш рюкзак переполнен!");
            return;
        }

        PlayerData playerData = wMine.getPlayerData(player);
        int reward = (int) (baseReward * playerData.costMultiplier());
        wMine.addBrokenBlock(player, reward);

        block.setType(Material.AIR);
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent("§fВы получили §6" + reward + "$ §fза §6" + material.name())
        );

        scheduleBlockRestore(block, material);
    }

    private boolean isBackpackFull(Player player) {
        PlayerData playerData = wMine.getPlayerData(player);
        return playerData.blocksBroken() >= playerData.backpack();
    }

    private void scheduleBlockRestore(Block block, Material material) {
        long stoneDelay = config.getLong("settings.stone-restore-delay", 1L);
        long respawnDelay = config.getLong("settings.block-respawn-delay", 200L);
        boolean randomPlace = config.getBoolean("randomPlace", false);

        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.STONE), stoneDelay);

        Material restoreMaterial = randomPlace ? getRandomConfiguredMaterial() : material;
        brokenBlocks.put(block, restoreMaterial);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Material type = brokenBlocks.remove(block);
            if (type != null) {
                block.setType(type);
            }
        }, respawnDelay);
    }

    private Material getRandomConfiguredMaterial() {
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            return Material.STONE;
        }

        List<String> keys = blocksSection.getKeys(false).stream().toList();
        if (keys.isEmpty()) {
            return Material.STONE;
        }

        int totalChance = 0;
        for (String key : keys) {
            totalChance += Math.max(0, blocksSection.getInt(key + ".chance", 0));
        }

        if (totalChance <= 0) {
            String randomKey = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
            Material fallbackMaterial = Material.matchMaterial(blocksSection.getString(randomKey + ".block", ""));
            return fallbackMaterial != null ? fallbackMaterial : Material.STONE;
        }

        int random = ThreadLocalRandom.current().nextInt(totalChance);
        int cumulativeChance = 0;
        for (String key : keys) {
            cumulativeChance += Math.max(0, blocksSection.getInt(key + ".chance", 0));
            if (random < cumulativeChance) {
                Material material = Material.matchMaterial(blocksSection.getString(key + ".block", ""));
                if (material != null) {
                    return material;
                }
            }
        }

        return Material.STONE;
    }

    private boolean isInMine(Location loc) {
        Location min = getMineLocation(true);
        Location max = getMineLocation(false);

        if (min == null || max == null || loc.getWorld() == null || !loc.getWorld().equals(min.getWorld())) {
            return false;
        }

        return loc.getX() >= min.getX() && loc.getX() <= max.getX()
                && loc.getY() >= min.getY() && loc.getY() <= max.getY()
                && loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ();
    }

    private Location getMineLocation(boolean minPoint) {
        String worldName = config.getString("location.world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            return null;
        }

        String xPath = minPoint ? "location.minX" : "location.maxX";
        String yPath = minPoint ? "location.minY" : "location.maxY";
        String zPath = minPoint ? "location.minZ" : "location.maxZ";

        return new Location(world, config.getDouble(xPath), config.getDouble(yPath), config.getDouble(zPath));
    }
}
