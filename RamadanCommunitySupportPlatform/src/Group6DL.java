// Group6DL.java - Data Layer for ISTE-330 Group 6
// Author: Ibraheem Mustafa
import java.util.Date;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import javax.swing.JOptionPane;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Group6DL {

    private static HikariDataSource dataSource;

    // Initialize the HikariCP connection pool
    public boolean connectDB() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mariadb://localhost:3308/group6db");
            config.setUsername("root");
            config.setPassword("mariadb");
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);

            // Statement pooling enhancements
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "50");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("Connected to group6db using HikariCP!");
            return true;

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    // Disconnect the pool
    public void disconnectDB() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("Disconnected from group6db.");
            }
        } catch (Exception e) {
            System.out.println("Disconnection error: " + e.getMessage());
        }
    }

    // Helper to get connection from pool
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Hash password using SHA-1
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return String.format("%040x", new BigInteger(1, hash));
        } catch (Exception e) {
            System.out.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }

    // Validate login and return role if successful
    public String login(String username, String rawPassword) {
        String role = null;
        String hashed = hashPassword(rawPassword);

        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                role = rs.getString("role");
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        return role;
    }

    // Transfer credits using stored procedure
    public boolean transferCredits(int fromAccountId, int toAccountId, double amount) {
        String call = "{CALL transfer_credits_by_account(?, ?, ?)}";
        try (Connection conn = getConnection(); CallableStatement stmt = conn.prepareCall(call)) {
            stmt.setInt(1, fromAccountId);
            stmt.setInt(2, toAccountId);
            stmt.setDouble(3, amount);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            System.out.println("Transfer failed: " + e.getMessage());
            return false;
        }
    }

    public boolean doesUserOwnAccount(String username, int accountId) {
        String sql = "SELECT 1 FROM accounts a JOIN users u ON a.user_id = u.user_id WHERE u.username = ? AND a.account_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, accountId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Ownership check failed: " + e.getMessage());
            return false;
        }
    }
    

    // View any table (admin only)
    public ResultSet viewTable(String tableName) {
        try {
            Connection conn = getConnection();
            CallableStatement stmt = conn.prepareCall("{CALL show_table(?)}");
            stmt.setString(1, tableName);
            stmt.execute();
            return stmt.getResultSet();
        } catch (SQLException e) {
            System.out.println("Error viewing table: " + e.getMessage());
            return null;
        }
    }

    // Add a new user (admin only)
    public boolean addUser(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        String hashed = hashPassword(password);

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.setString(3, role);
            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.out.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    // Fetch all client info (accounts, requests, transfers)
    public String fetchClientInfo(String username) {
        StringBuilder sb = new StringBuilder();
        String userQuery = "SELECT user_id FROM users WHERE username = ?";
        String accountQuery = "SELECT account_id, account_type, balance FROM accounts WHERE user_id = ?";
        String requestQuery = "SELECT amount, reason, status, request_date FROM requests WHERE user_id = ?";
        String transferQuery = "SELECT transfer_id, from_account, to_account, amount, timestamp FROM transfers " +
                "WHERE from_account IN (SELECT account_id FROM accounts WHERE user_id = ?) " +
                "OR to_account IN (SELECT account_id FROM accounts WHERE user_id = ?)";

        try (Connection conn = getConnection();
                PreparedStatement userStmt = conn.prepareStatement(userQuery)) {

            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next())
                return "User not found.";

            int userId = userRs.getInt("user_id");

            // Accounts
            sb.append("=== Accounts ===\n");
            try (PreparedStatement accStmt = conn.prepareStatement(accountQuery)) {
                accStmt.setInt(1, userId);
                ResultSet accRs = accStmt.executeQuery();
                while (accRs.next()) {
                    sb.append("Account ID: ").append(accRs.getInt("account_id"))
                            .append(" | Type: ").append(accRs.getString("account_type"))
                            .append(" | Balance: ").append(accRs.getDouble("balance")).append("\n");
                }
            }

            // Requests
            sb.append("\n=== Requests ===\n");
            try (PreparedStatement reqStmt = conn.prepareStatement(requestQuery)) {
                reqStmt.setInt(1, userId);
                ResultSet reqRs = reqStmt.executeQuery();
                while (reqRs.next()) {
                    sb.append("Amount: ").append(reqRs.getDouble("amount"))
                            .append(" | Reason: ").append(reqRs.getString("reason"))
                            .append(" | Status: ").append(reqRs.getString("status"))
                            .append(" | Date: ").append(reqRs.getTimestamp("request_date")).append("\n");
                }
            }

            // Transfers
            sb.append("\n=== Transfers (as Sender or Receiver) ===\n");
            try (PreparedStatement transStmt = conn.prepareStatement(transferQuery)) {
                transStmt.setInt(1, userId);
                transStmt.setInt(2, userId);
                ResultSet transRs = transStmt.executeQuery();
                while (transRs.next()) {
                    sb.append("Transfer ID: ").append(transRs.getInt("transfer_id"))
                            .append(" | From: ").append(transRs.getInt("from_account"))
                            .append(" | To: ").append(transRs.getInt("to_account"))
                            .append(" | Amount: ").append(transRs.getDouble("amount"))
                            .append(" | Date: ").append(transRs.getTimestamp("timestamp")).append("\n");
                }
            }

        } catch (SQLException e) {
            return "❌ Error fetching info: " + e.getMessage();
        }

        return sb.toString();
    }

    // Insert aid request
    public boolean insertAidRequest(String username, double amount, String reason) {
        String findUser = "SELECT user_id FROM users WHERE username = ?";
        String insertRequest = "INSERT INTO requests (user_id, amount, reason, status) VALUES (?, ?, ?, 'pending')";

        try (Connection conn = getConnection();
                PreparedStatement userStmt = conn.prepareStatement(findUser)) {

            userStmt.setString(1, username);
            ResultSet rs = userStmt.executeQuery();

            if (!rs.next())
                return false;
            int userId = rs.getInt("user_id");

            try (PreparedStatement insertStmt = conn.prepareStatement(insertRequest)) {
                insertStmt.setInt(1, userId);
                insertStmt.setDouble(2, amount);
                insertStmt.setString(3, reason);
                return insertStmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            System.out.println("Aid request insert failed: " + e.getMessage());
            return false;
        }
    }

    // Create a new account for a user (admin only)
    public boolean createAccount(int userId, String accountType, double balance) {
        String sql = "INSERT INTO accounts (user_id, account_type, balance) VALUES (?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, accountType);
            stmt.setDouble(3, balance);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
            return false;
        }
    }

    public void performBackup() {
        try {
            String timestamp = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date());
            String backupFile = "group6db_BACKUP_" + timestamp + ".sql";
            String dumpPath = "\"C:\\Program Files\\MariaDB 11.4\\bin\\mysqldump.exe\"";
            String command = dumpPath + " -u root -pmariadb group6db -P 3308 -h localhost -r " + backupFile;

            Process runtimeProcess = Runtime.getRuntime().exec(command);
            int processComplete = runtimeProcess.waitFor();

            if (processComplete == 0) {
                JOptionPane.showMessageDialog(null, "✅ Backup successful: " + backupFile);
            } else {
                JOptionPane.showMessageDialog(null, "❌ Backup failed. Exit code: " + processComplete);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "❌ Error during backup: " + e.getMessage());
        }
    }

}
