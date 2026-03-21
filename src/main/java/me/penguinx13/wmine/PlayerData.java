package me.penguinx13.wmine;

import me.penguinx13.wapi.managers.SQLiteManager;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public record PlayerData(int backpack, double costMultiplier, int earnings, int blocksBroken) {

    public static PlayerData getOrCreate(Player player) {
        return getOrCreate(player.getUniqueId().toString(), player.getName());
    }

    public static PlayerData getOrCreate(String uuid, String playerName) {
        WMine plugin = WMine.getInstance();
        int defaultBackpack = plugin.getMainConfig().getInt("defaultValues.backpack");
        double defaultMultiplier = plugin.getMainConfig().getDouble("defaultValues.costmultiplier");

        SQLiteManager sqliteManager = plugin.getSqliteManager();
        try (PreparedStatement statement = sqliteManager.prepareStatement(
                "SELECT backpack, costmultiplier, earnings, blocksBroken FROM players WHERE uuid = ?",
                uuid
        ); ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return new PlayerData(
                        resultSet.getInt("backpack"),
                        resultSet.getDouble("costmultiplier"),
                        resultSet.getInt("earnings"),
                        resultSet.getInt("blocksBroken")
                );
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("SQL error while reading player data: " + exception.getMessage());
        }

        sqliteManager.executeUpdate(
                "INSERT INTO players (uuid, name, backpack, costmultiplier, earnings, blocksBroken) VALUES (?, ?, ?, ?, 0, 0)",
                uuid,
                playerName,
                defaultBackpack,
                defaultMultiplier
        );

        return new PlayerData(defaultBackpack, defaultMultiplier, 0, 0);
    }

    public static void addBrokenBlock(Player player, int reward) {
        WMine plugin = WMine.getInstance();
        PlayerData playerData = getOrCreate(player);
        plugin.getSqliteManager().executeUpdate(
                "UPDATE players SET name = ?, blocksBroken = ?, earnings = ? WHERE uuid = ?",
                player.getName(),
                playerData.blocksBroken() + 1,
                playerData.earnings() + reward,
                player.getUniqueId().toString()
        );
    }

    public static double getParameter(String uuid, String playerName, String parameter) {
        PlayerData playerData = getOrCreate(uuid, playerName);
        if ("backpack".equals(parameter)) {
            return playerData.backpack();
        }
        return playerData.costMultiplier();
    }

    public static void setParameter(String uuid, String playerName, String parameter, double value) {
        WMine plugin = WMine.getInstance();
        if ("backpack".equals(parameter)) {
            int backpack = Math.max(0, (int) Math.round(value));
            plugin.getSqliteManager().executeUpdate(
                    "UPDATE players SET name = ?, backpack = ? WHERE uuid = ?",
                    playerName,
                    backpack,
                    uuid
            );
            return;
        }

        plugin.getSqliteManager().executeUpdate(
                "UPDATE players SET name = ?, costmultiplier = ? WHERE uuid = ?",
                playerName,
                Math.max(0, value),
                uuid
        );
    }

    public static void resetClaimData(Player player) {
        WMine plugin = WMine.getInstance();
        plugin.getSqliteManager().executeUpdate(
                "UPDATE players SET earnings = 0, blocksBroken = 0, name = ? WHERE uuid = ?",
                player.getName(),
                player.getUniqueId().toString()
        );
    }
}
