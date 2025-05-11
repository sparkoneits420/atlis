package org.atlis.server.net.sql;

import org.atlis.server.net.http.PassCheck;
import org.atlis.common.model.Player;

import java.sql.*;
import org.atlis.common.util.Constants;

public class Database {

    private static Connection connection;

    public static void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager
                    .getConnection(System.getProperty("DB_URL"),
                            System.getProperty("DB_USER"),
                            System.getProperty("DB_PASS"));
        }
    }

    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } 
    }

    public static Player loadPlayer(String username, String password) { 
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT user_id, username, user_password FROM phpbb_users WHERE username_clean = ?"
            );
            ps.setString(1, username.toLowerCase());
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();  
            if (!found) { 
                return null;
            } 
            int userId = rs.getInt("user_id");
            String actualUsername = rs.getString("username"); // for casing
            String hash = rs.getString("user_password"); 
            if (!PassCheck.checkLogin(username, password)) { 
                return null;
            }

            // Step 2: Load game data from player_data
            Player player = new Player();
            player.setUsername(actualUsername);
            player.setPassword(hash);
            player.setId(userId);

            ps = connection.prepareStatement("SELECT * FROM player_data WHERE user_id = ?");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                player.setX(rs.getInt("x"));
                player.setY(rs.getInt("y"));
                player.setZ(rs.getInt("z")); 
                player.lastRegionX = rs.getInt("last_region_x");
                player.lastRegionY = rs.getInt("last_region_y");
            }
            
            if(player.x == 0 && player.y == 0) {
                player.x = Constants.START_X;
                player.y = Constants.START_Y;
            }

            return player;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void savePlayer(Player player) {
        if (player == null) {
            return;
        }

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "REPLACE INTO player_data (user_id, x, y, z, last_region_x, last_region_y) "
                    + "VALUES (?, ?, ?, ?, ?, ?)"
            );

            ps.setLong(1, player.getId());
            ps.setInt(2, player.getX());
            ps.setInt(3, player.getY());
            ps.setInt(4, player.getZ()); 
            ps.setInt(5, player.lastRegionX);
            ps.setInt(6, player.lastRegionY);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean playerExists(String username) {
        String query = "SELECT 1 FROM phpbb_users WHERE username_clean = ? LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // returns true if a row was found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
