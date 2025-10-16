package me.penguinx13.wmine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class BlockBreakListener implements Listener {
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
        Location blockLocation = event.getBlock().getLocation();

        UUID playerId = player.getUniqueId();
        int blocksBrokenByPlayer = config.blocksBroken.getOrDefault(playerId, 0);

        if (isLocationInRange(blockLocation, config.minLocation, config.maxLocation)) {
            Material blockMaterial = brokenBlock.getType();

            if (data != null) {
            	ConfigurationSection playerSection = data.getDataConfig().getConfigurationSection("players." + player.getName());

            	if (config.blockRewards.containsKey(blockMaterial)) {
            	    double reward = config.blockRewards.get(blockMaterial);
            	    double costMultiplier = playerSection != null ? playerSection.getDouble("costmultiplier", config.costmultiplier) : plugin.getConfig().getDouble("defaultValues.costmultiplier");
            	    int backpackSize = playerSection != null ? playerSection.getInt("backpack", config.backpack) : plugin.getConfig().getInt("defaultValues.backpack");
                        if (blocksBrokenByPlayer < backpackSize) {
                            if (reward > 0) {
                                config.blocksBroken.put(playerId, blocksBrokenByPlayer + 1);
                                double currentEarnings = config.playerEarnings.getOrDefault(playerId, 0);
                                reward *= costMultiplier;
                                currentEarnings += reward;
                                int newEarnings = (int) currentEarnings;
                                config.playerEarnings.put(playerId, newEarnings);

                                brokenBlock.setType(Material.AIR);

                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§fВы сломали §6" + blockMaterial + "§f и получили §6" + reward + "$"));

                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    event.getBlock().setType(Material.STONE);
                                }, 1L);

                                if (config.randomPlace) {
                                    List<Material> rewardMaterials = new ArrayList<>();
                                    for (String blockKey : config.getBlockKeys()) {
                                        ConfigurationSection blockData = config.getBlockData(blockKey);
                                        int chance = blockData.getInt("chance");
                                        for (int i = 0; i < chance; i++) {
                                            rewardMaterials.add(Material.matchMaterial(Objects.requireNonNull(blockData.getString("block"))));
                                        }
                                    }
                                    Material randomMaterial = rewardMaterials.get(new Random().nextInt(rewardMaterials.size()));
                                    config.brokenBlocks.put(brokenBlock, randomMaterial);
                                } else {
                                    config.brokenBlocks.put(brokenBlock, blockMaterial);
                                }

                                startTimer(brokenBlock);
                            } else {
                                player.sendMessage("Ошибка при получении валюты");
                            }
                        } else {
                            event.setCancelled(true);
                            player.sendMessage("§cВы переполнены, сдайте ресурсы!");
                        }
                } else {
                    event.setCancelled(true);
                }
            } else {
                plugin.getLogger().info("data = null"); // Обработка случая, когда data равно null
            }
        }
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
                if (config.brokenBlocks.containsKey(block)) {
                    block.setType(config.brokenBlocks.get(block));
                    config.brokenBlocks.remove(block);
                }
            }
        }.runTaskLater(plugin, 200L);
    }
}