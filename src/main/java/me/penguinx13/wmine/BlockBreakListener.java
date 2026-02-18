package me.penguinx13.wmine;

import me.penguinx13.wapi.Managers.ConfigManager;
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
import java.util.Map;

public class BlockBreakListener implements Listener {

    private static final String PLAYERS_PATH = "players.";

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
        int baseReward = getBaseReward(material);

        if (baseReward <= 0) {
            event.setCancelled(true);
            return;
        }

        ConfigurationSection playerSection = getPlayerSection(player);

        int broken = playerSection.getInt("blocksBroken", 0);
        double multiplier = playerSection.getDouble("costmultiplier",
                config.getDouble("defaultValues.costmultiplier"));
        int backpack = playerSection.getInt("backpack",
                config.getInt("defaultValues.backpack"));

        if (broken >= backpack) {
            event.setCancelled(true);
            player.sendMessage("§cВаш рюкзак переполнен!");
            return;
        }

        int reward = (int) (baseReward * multiplier);
        playerSection.set("blocksBroken", broken + 1);
        playerSection.set("earnings", playerSection.getInt("earnings", 0) + reward);
        wMine.saveDataConfig();

        block.setType(Material.AIR);

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent("§fВы получили §6" + reward + "$ §fза §6" + material.name())
        );

        long stoneDelay = config.getLong("settings.stone-restore-delay", 1L);
        long respawnDelay = config.getLong("settings.block-respawn-delay", 200L);

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> block.setType(Material.STONE), stoneDelay);

        Material nextMaterial = material;
        brokenBlocks.put(block, nextMaterial);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Material type = brokenBlocks.remove(block);
            if (type != null) {
                block.setType(type);
            }
        }, respawnDelay);
    }

    private ConfigurationSection getPlayerSection(Player player) {
        String path = PLAYERS_PATH + player.getName();
        FileConfiguration dataConfig = wMine.getDataConfig();
        ConfigurationSection playerSection = dataConfig.getConfigurationSection(path);

        if (playerSection == null) {
            playerSection = dataConfig.createSection(path);
            playerSection.set("backpack", config.getInt("defaultValues.backpack"));
            playerSection.set("costmultiplier", config.getDouble("defaultValues.costmultiplier"));
            playerSection.set("earnings", 0);
            playerSection.set("blocksBroken", 0);
            wMine.saveDataConfig();
        }

        return playerSection;
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

    private int getBaseReward(Material material) {
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            return -1;
        }

        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
            if (blockSection == null) {
                continue;
            }

            Material configuredMaterial = Material.matchMaterial(blockSection.getString("block", ""));
            if (configuredMaterial == material) {
                return blockSection.getInt("cost", -1);
            }
        }

        return -1;
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

        return new Location(world,
                config.getDouble(xPath),
                config.getDouble(yPath),
                config.getDouble(zPath)
        );
    }
}
