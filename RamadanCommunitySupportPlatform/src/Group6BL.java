// Group6BL.java - Business Layer for ISTE-330 Group 6
// Author: Ibraheem Mustafa

import java.sql.ResultSet;

public class Group6BL {

    private Group6DL dataLayer;

    public Group6BL() {
        dataLayer = new Group6DL();
    }

    // Connect to DB
    public boolean connect() {
        return dataLayer.connectDB();
    }

    // Disconnect from DB
    public void disconnect() {
        dataLayer.disconnectDB();
    }

    // Login and return role (admin/client), or null if failed
    public String loginUser(String username, String password) {
        return dataLayer.login(username, password);
    }

    // Add new user (admin or client)
    public boolean addNewUser(String username, String password, String role) {
        if (!role.equals("admin") && !role.equals("client")) {
            System.out.println("Invalid role type!");
            return false;
        }
        return dataLayer.addUser(username, password, role);
    }

    // Perform credit transfer between accounts
    public boolean transferBetweenAccounts(String username, int fromAccId, int toAccId, double amount) {
        if (amount <= 0) {
            System.out.println("Amount must be greater than 0.");
            return false;
        }

        // Check account ownership
        if (!dataLayer.doesUserOwnAccount(username, fromAccId)) {
            System.out.println("Unauthorized transfer attempt from non-owned account.");
            return false;
        }
        return dataLayer.transferCredits(fromAccId, toAccId, amount);
    }

    // View any table – admin only
    public ResultSet viewAnyTable(String tableName) {
        return dataLayer.viewTable(tableName);
    }

    // NEW: View personal client info (accounts, requests, transfers)
    public String getClientDetails(String username) {
        System.out.println("Viewing info for: " + username);

        return dataLayer.fetchClientInfo(username);
    }

    // NEW: Submit an aid request
    public boolean submitAidRequest(String username, double amount, String reason) {
        return dataLayer.insertAidRequest(username, amount, reason);
    }

    // NEW: Admin creates an account for a user
    public boolean createAccountForUser(int userId, String accountType, double balance) {
        if (accountType == null || accountType.isEmpty() || balance < 0) {
            System.out.println("Invalid account type or balance.");
            return false;
        }
        return dataLayer.createAccount(userId, accountType, balance);
    }

    public void backupDatabase() {
        dataLayer.performBackup();
    }

}
