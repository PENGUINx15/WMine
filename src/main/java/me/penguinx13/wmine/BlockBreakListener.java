package me.penguinx13.wmine;

import me.penguinx13.wapi.Managers.ConfigManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.UUID;

public class BlockBreakListener implements Listener {

    private final Plugin plugin;

    private final FileConfiguration config;
    private final FileConfiguration data;

    private final Map<UUID, Integer> blocksBroken = new HashMap<>();
    private final Map<Block, Material> brokenBlocks = new HashMap<>();

    public BlockBreakListener(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;

        this.config = configManager.getConfig("config.yml");
        this.data = configManager.getConfig("data.yml");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!isInMine(block.getLocation())) {
            return;
        }

        Material material = block.getType();
        int baseReward = config.getInt("rewards." + material.name(), -1);

        if (baseReward <= 0) {
            event.setCancelled(true);
            return;
        }

        UUID uuid = player.getUniqueId();
        int broken = blocksBroken.getOrDefault(uuid, 0);

        ConfigurationSection playerSection =
                data.getConfigurationSection("players." + player.getName());

        double multiplier = playerSection != null
                ? playerSection.getDouble("costmultiplier",
                config.getDouble("defaults.costmultiplier"))
                : config.getDouble("defaults.costmultiplier");

        int backpack = playerSection != null
                ? playerSection.getInt("backpack",
                config.getInt("defaults.backpack"))
                : config.getInt("defaults.backpack");

        if (broken >= backpack) {
            event.setCancelled(true);
            player.sendMessage("§cВаш рюкзак переполнен!");
            return;
        }

        int reward = (int) (baseReward * multiplier);

        blocksBroken.put(uuid, broken + 1);

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

    private boolean isInMine(Location loc) {
        Location min = getLocation("mine.min");
        Location max = getLocation("mine.max");

        return loc.getX() >= min.getX() && loc.getX() <= max.getX()
                && loc.getY() >= min.getY() && loc.getY() <= max.getY()
                && loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ();
    }

    private Location getLocation(String path) {
        return new Location(
                Bukkit.getWorlds().get(0),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z")
        );
    }
}
