package me.penguinx13.wapi.Managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLiteManager {

    private final JavaPlugin plugin;
    private final String databaseName;

    private Connection connection;

    public SQLiteManager(JavaPlugin plugin, String databaseName) {
        this.plugin = plugin;
        this.databaseName = databaseName;
    }

    public void connect() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            File databaseFile = new File(plugin.getDataFolder(), databaseName);
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to connect to SQLite: " + exception.getMessage());
        }
    }

    public void disconnect() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to close SQLite connection: " + exception.getMessage());
        }
    }

    public PreparedStatement prepareStatement(String query, Object... parameters) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        bindParameters(preparedStatement, parameters);
        return preparedStatement;
    }

    public void executeUpdate(String query, Object... parameters) {
        try (PreparedStatement preparedStatement = prepareStatement(query, parameters)) {
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("SQLite update failed: " + exception.getMessage());
        }
    }

    private void bindParameters(PreparedStatement preparedStatement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            preparedStatement.setObject(i + 1, parameters[i]);
        }
    }
}
