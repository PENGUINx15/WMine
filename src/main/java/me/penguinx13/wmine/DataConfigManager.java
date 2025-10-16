package me.penguinx13.wmine;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DataConfigManager {
    private final JavaPlugin plugin;
    private final String dataFileName = "data.yml";
    private FileConfiguration dataConfig;
    private final File dataFile;

    public DataConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), dataFileName);
    }

    public void setupDataConfig() {
        if (!dataFile.exists()) {
            plugin.saveResource(dataFileName, false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить data.yml");
        }
    }

    public void reload() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean exists() {
        return dataFile.exists();
    }
}