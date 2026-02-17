package me.penguinx13.wmine;

import java.util.UUID;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockBreakListener implements Listener {
    private static final long STONE_RESTORE_DELAY = 1L;
    private static final long BLOCK_RESPAWN_DELAY = 200L;

    private final ConfigManager config;
    private final WMine plugin;
    private final DataConfigManager data;

    public BlockBreakListener(ConfigManager config, WMine plugin, DataConfigManager data) {
        this.config = config;
        this.plugin = plugin;
        this.data = data;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();

        if (!isLocationInRange(brokenBlock.getLocation(), config.minLocation, config.maxLocation)) {
            return;
        }

        Material blockMaterial = brokenBlock.getType();
        Integer baseReward = config.blockRewards.get(blockMaterial);
        if (baseReward == null) {
            event.setCancelled(true);
            return;
        }

        UUID playerId = player.getUniqueId();
        int blocksBrokenByPlayer = config.blocksBroken.getOrDefault(playerId, 0);

        ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + player.getName());
        double costMultiplier = playerSection != null
                ? playerSection.getDouble("costmultiplier", config.costmultiplier)
                : config.costmultiplier;
        int backpackSize = playerSection != null
                ? playerSection.getInt("backpack", config.backpack)
                : config.backpack;

        if (blocksBrokenByPlayer >= backpackSize) {
            event.setCancelled(true);
            player.sendMessage("§cВы переполнены, сдайте ресурсы!");
            return;
        }

        double reward = baseReward * costMultiplier;
        if (reward <= 0) {
            player.sendMessage("Ошибка при получении валюты");
            return;
        }

        config.blocksBroken.put(playerId, blocksBrokenByPlayer + 1);
        int currentEarnings = config.playerEarnings.getOrDefault(playerId, 0);
        config.playerEarnings.put(playerId, currentEarnings + (int) reward);

        brokenBlock.setType(Material.AIR);
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent("§fВы сломали §6" + blockMaterial + "§f и получили §6" + reward + "$")
        );

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> brokenBlock.setType(Material.STONE), STONE_RESTORE_DELAY);

        Material nextMaterial = config.randomPlace
                ? config.getRandomRewardMaterial(blockMaterial)
                : blockMaterial;

        config.brokenBlocks.put(brokenBlock, nextMaterial);
        startTimer(brokenBlock);
    }

    public boolean isLocationInRange(Location location, Location minLocation, Location maxLocation) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minLocation.getX() && x <= maxLocation.getX()
                && y >= minLocation.getY() && y <= maxLocation.getY()
                && z >= minLocation.getZ() && z <= maxLocation.getZ();
    }

    private void startTimer(Block block) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Material blockType = config.brokenBlocks.remove(block);
                if (blockType != null) {
                    block.setType(blockType);
                }
            }
        }.runTaskLater(plugin, BLOCK_RESPAWN_DELAY);
    }
}
