// Group6DL.java - Data Layer for ISTE-330 Group 6
// Author: Ibraheem Mustafa

import java.sql.*;
import java.security.MessageDigest;
import java.math.BigInteger;

public class Group6DL {

    private Connection conn;

    // Connect to the database
    public boolean connectDB() {
        try {
            String url = "jdbc:mariadb://localhost:3308/group6db";
            String user = "root";
            String password = "mariadb"; // Prompt if needed

            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);

            System.out.println("Connected to group6db!");
            return true;

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    // Disconnect from the database
    public void disconnectDB() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Disconnected from group6db.");
            }
        } catch (Exception e) {
            System.out.println("Disconnection error: " + e.getMessage());
        }
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

    // Validate login and return role if successful, else null
    public String login(String username, String rawPassword) {
        String role = null;
        String hashed = hashPassword(rawPassword);

        try {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT role FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, hashed);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                role = rs.getString("role");
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        return role;
    }

    // Transfer credits using stored procedure
    public boolean transferCredits(int fromAccountId, int toAccountId, double amount) {
        try {
            CallableStatement stmt = conn.prepareCall(
                "{CALL transfer_credits_by_account(?, ?, ?)}");
            stmt.setInt(1, fromAccountId);
            stmt.setInt(2, toAccountId);
            stmt.setDouble(3, amount);

            stmt.execute();
            stmt.close();
            return true;

        } catch (SQLException e) {
            System.out.println("Transfer failed: " + e.getMessage());
            return false;
        }
    }

    // View any table (used by admin)
    public ResultSet viewTable(String tableName) {
        try {
            CallableStatement stmt = conn.prepareCall("{CALL show_table(?)}");
            stmt.setString(1, tableName);
            stmt.execute();
            return stmt.getResultSet();
        } catch (SQLException e) {
            System.out.println("Error viewing table: " + e.getMessage());
            return null;
        }
    }

    // Used to create a new user (admin use)
    public boolean addUser(String username, String password, String role) {
        try {
            String hashed = hashPassword(password);
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.setString(3, role);

            int result = stmt.executeUpdate();
            stmt.close();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    // NEW: Fetch all info (accounts, requests, transfers) for a client
    public String fetchClientInfo(String username) {
        StringBuilder sb = new StringBuilder();

        try {
            // Step 1: Get user_id
            PreparedStatement userStmt = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?");
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next()) {
                return "User not found.";
            }
            int userId = userRs.getInt("user_id");
            userRs.close();
            userStmt.close();

            // Step 2: Accounts
            sb.append("=== Accounts ===\n");
            PreparedStatement accStmt = conn.prepareStatement(
                "SELECT account_id, account_type, balance FROM accounts WHERE user_id = ?");
            accStmt.setInt(1, userId);
            ResultSet accRs = accStmt.executeQuery();

            while (accRs.next()) {
                sb.append("Account ID: ").append(accRs.getInt("account_id"))
                  .append(" | Type: ").append(accRs.getString("account_type"))
                  .append(" | Balance: ").append(accRs.getDouble("balance")).append("\n");
            }
            accRs.close();
            accStmt.close();

            // Step 3: Requests
            sb.append("\n=== Requests ===\n");
            PreparedStatement reqStmt = conn.prepareStatement(
                "SELECT amount, reason, status, request_date FROM requests WHERE user_id = ?");
            reqStmt.setInt(1, userId);
            ResultSet reqRs = reqStmt.executeQuery();

            while (reqRs.next()) {
                sb.append("Amount: ").append(reqRs.getDouble("amount"))
                  .append(" | Reason: ").append(reqRs.getString("reason"))
                  .append(" | Status: ").append(reqRs.getString("status"))
                  .append(" | Date: ").append(reqRs.getTimestamp("request_date")).append("\n");
            }
            reqRs.close();
            reqStmt.close();

            // Step 4: Transfers
            sb.append("\n=== Transfers (as Sender or Receiver) ===\n");
            PreparedStatement transStmt = conn.prepareStatement(
                "SELECT transfer_id, from_account, to_account, amount, timestamp FROM transfers " +
                "WHERE from_account IN (SELECT account_id FROM accounts WHERE user_id = ?) " +
                "OR to_account IN (SELECT account_id FROM accounts WHERE user_id = ?)");
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
            transRs.close();
            transStmt.close();

        } catch (SQLException e) {
            return "❌ Error fetching info: " + e.getMessage();
        }

        return sb.toString();
    }

    // NEW: Insert aid request
    public boolean insertAidRequest(String username, double amount, String reason) {
        try {
            // Get user ID
            PreparedStatement userStmt = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?");
            userStmt.setString(1, username);
            ResultSet rs = userStmt.executeQuery();

            if (!rs.next()) return false;
            int userId = rs.getInt("user_id");
            rs.close();
            userStmt.close();

            // Insert request
            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO requests (user_id, amount, reason, status) VALUES (?, ?, ?, 'pending')");
            insertStmt.setInt(1, userId);
            insertStmt.setDouble(2, amount);
            insertStmt.setString(3, reason);

            int result = insertStmt.executeUpdate();
            insertStmt.close();
            return result > 0;

        } catch (SQLException e) {
            System.out.println("Aid request insert failed: " + e.getMessage());
            return false;
        }
    }
}
