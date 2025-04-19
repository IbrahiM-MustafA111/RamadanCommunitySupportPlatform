// Group6PL.java – Presentation Layer for ISTE-330 Group 6
// Author: Ibraheem Mustafa

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Group6PL extends JFrame {

    private Group6BL business;
    private JTextField tfUser;
    private JPasswordField tfPass;

    public Group6PL() {
        super("Login – Ramadan Support Platform");
        business = new Group6BL();
        buildLoginUI();
    }

    private void buildLoginUI() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(Color.decode("#FFF9DB")); // soft yellow
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 13));
        tfUser = new JTextField(12);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 13));
        tfPass = new JPasswordField(12);

        JButton btnConnect = new JButton("Connect");
        JButton btnDisconnect = new JButton("Disconnect");
        JButton btnLogin = new JButton("Login");

        // Row 1 – Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        loginPanel.add(lblUser, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        loginPanel.add(tfUser, gbc);

        // Row 2 – Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        loginPanel.add(lblPass, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        loginPanel.add(tfPass, gbc);

        // Row 3 – Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.decode("#FFF9DB"));
        buttonPanel.add(btnConnect);
        buttonPanel.add(btnDisconnect);
        buttonPanel.add(btnLogin);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(buttonPanel, gbc);

        add(loginPanel);

        // Actions
        btnConnect.addActionListener(e -> {
            boolean ok = business.connect();
            JOptionPane.showMessageDialog(this, ok ? "Connected!" : "Connection failed.");
        });

        btnDisconnect.addActionListener(e -> {
            business.disconnect();
            JOptionPane.showMessageDialog(this, "Disconnected.");
        });

        btnLogin.addActionListener(e -> {
            String username = tfUser.getText();
            String password = new String(tfPass.getPassword());
            String role = business.loginUser(username, password);

            if (role == null) {
                JOptionPane.showMessageDialog(this, "Invalid login.");
            } else {
                JOptionPane.showMessageDialog(this, "Logged in as " + role);
                dispose();
                if (role.equals("admin")) {
                    new AdminMenu();
                } else {
                    new ClientMenu(username); // if you added that param
                }
            }
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 180);
        setLocationRelativeTo(null); // center the window
        setVisible(true);
    }

    private class AdminMenu extends JFrame {
        private JComboBox<String> tableList;
    
        public AdminMenu() {
            super("Admin Menu");
            buildAdminUI();
        }
    
        private void buildAdminUI() {
            setLayout(new BorderLayout());
            getContentPane().setBackground(Color.decode("#FFF9DB"));
    
            // === Header ===
            JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
            header.setBackground(Color.decode("#FFF9DB"));
            JLabel iconLabel = new JLabel();
    
            try {
                ImageIcon icon = new ImageIcon("src/pics/crescent.png");
                Image scaled = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                System.out.println("Icon load failed: " + e.getMessage());
            }
    
            JLabel welcome = new JLabel("Welcome to the Admin Panel");
            welcome.setFont(new Font("Serif", Font.BOLD, 18));
            welcome.setForeground(new Color(75, 50, 0));
    
            header.add(iconLabel);
            header.add(welcome);
            add(header, BorderLayout.NORTH);
    
            // === Center Panel (Buttons) ===
            JPanel center = new JPanel();
            center.setBackground(Color.decode("#FFF9DB"));
    
            tableList = new JComboBox<>(new String[]{"users", "accounts", "transfers", "requests", "admin_logs"});
            JButton btnViewTable = new JButton("View Table");
            JButton btnAddUser = new JButton("Add User");
            JButton btnCreateAccount = new JButton("Create Account"); // NEW BUTTON
            JButton btnBackup = new JButton("Backup DB");
    
            // View any table
            btnViewTable.addActionListener(e -> {
                String table = (String) tableList.getSelectedItem();
                ResultSet rs = business.viewAnyTable(table);
                if (rs == null) {
                    JOptionPane.showMessageDialog(this, "❌ Failed to load table: " + table);
                    return;
                }
    
                try {
                    StringBuilder sb = new StringBuilder();
                    ResultSetMetaData md = rs.getMetaData();
                    while (rs.next()) {
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            sb.append(md.getColumnName(i)).append(": ").append(rs.getString(i)).append("  ");
                        }
                        sb.append("\n");
                    }
    
                    JTextArea outputArea = new JTextArea(sb.toString(), 20, 50);
                    outputArea.setWrapStyleWord(true);
                    outputArea.setLineWrap(true);
                    outputArea.setCaretPosition(0);
                    outputArea.setEditable(false);
                    JScrollPane scrollPane = new JScrollPane(outputArea);
    
                    JOptionPane.showMessageDialog(this, scrollPane, "📋 Table: " + table, JOptionPane.INFORMATION_MESSAGE);
    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
                }
            });
    
            // Add user
            btnAddUser.addActionListener(e -> {
                String u = JOptionPane.showInputDialog(this, "Username:");
                String p = JOptionPane.showInputDialog(this, "Password:");
                String r = JOptionPane.showInputDialog(this, "Role (admin/client):");
                boolean ok = business.addNewUser(u, p, r);
                JOptionPane.showMessageDialog(this, ok ? "✅ User added." : "❌ Add failed.");
            });
    
            // Create account (NEW)
            btnCreateAccount.addActionListener(e -> {
                try {
                    String userStr = JOptionPane.showInputDialog(this, "Enter User ID:");
                    if (userStr == null) return;
                    int userId = Integer.parseInt(userStr.trim());
    
                    String type = JOptionPane.showInputDialog(this, "Enter Account Type:");
                    if (type == null || type.isEmpty()) return;
    
                    String balanceStr = JOptionPane.showInputDialog(this, "Enter Initial Balance:");
                    if (balanceStr == null) return;
                    double balance = Double.parseDouble(balanceStr.trim());
    
                    boolean ok = business.createAccountForUser(userId, type, balance);
                    JOptionPane.showMessageDialog(this, ok ? "✅ Account created." : "❌ Account creation failed.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "❌ Invalid input.");
                }
            });

            btnBackup.addActionListener(e -> business.backupDatabase());
    
            center.add(tableList);
            center.add(btnViewTable);
            center.add(btnAddUser);
            center.add(btnCreateAccount); // ADD TO UI
            center.add(btnBackup);
    
            add(center, BorderLayout.CENTER);
    
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }
    
    

    // ================= CLIENT MENU ===================
    private class ClientMenu extends JFrame {
        private String username;

        public ClientMenu(String username) {
            super("Client Menu");
            this.username = username;
            buildClientUI();
        }

        private void buildClientUI() {
            setLayout(new BorderLayout());
            getContentPane().setBackground(Color.decode("#FFF9DB"));

            // === Header ===
            JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
            header.setBackground(Color.decode("#FFF9DB"));
            JLabel iconLabel = new JLabel();

            try {
                ImageIcon icon = new ImageIcon("src/pics/crescent.png");
                Image scaled = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                System.out.println("Icon load failed: " + e.getMessage());
            }

            JLabel welcome = new JLabel("Welcome to the Ramadan Support Platform");
            welcome.setFont(new Font("Serif", Font.BOLD, 18));
            welcome.setForeground(new Color(75, 50, 0));

            header.add(iconLabel);
            header.add(welcome);
            add(header, BorderLayout.NORTH);

            // === Buttons ===
            JPanel center = new JPanel();
            center.setBackground(Color.decode("#FFF9DB"));
            JButton btnTransfer = new JButton("Transfer Credits");
            JButton btnView = new JButton("View Info");
            JButton btnRequest = new JButton("Request Aid");

            center.add(btnTransfer);
            center.add(btnView);
            center.add(btnRequest);
            add(center, BorderLayout.CENTER);

            // === Transfer Logic ===
            btnTransfer.addActionListener(e -> {
                try {
                    String fromStr = JOptionPane.showInputDialog(this, "Enter From Account ID:");
                    if (fromStr == null)
                        return;
                    int from = Integer.parseInt(fromStr.trim());

                    String toStr = JOptionPane.showInputDialog(this, "Enter To Account ID:");
                    if (toStr == null)
                        return;
                    int to = Integer.parseInt(toStr.trim());

                    String amtStr = JOptionPane.showInputDialog(this, "Enter Amount to Transfer:");
                    if (amtStr == null)
                        return;
                    double amt = Double.parseDouble(amtStr.trim());

                    boolean ok = business.transferBetweenAccounts(username, from, to, amt);
                    JOptionPane.showMessageDialog(this, ok ? "✅ Transfer successful!" : "❌ Transfer failed.");

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "❌ Invalid input.");
                }
            });

            // === View Info Logic ===
            btnView.addActionListener(e -> {
                StringBuilder sb = new StringBuilder();
                sb.append("📘 Your Information\n\n");

                String result = business.getClientDetails(username);
                sb.append(result);

                JTextArea textArea = new JTextArea(sb.toString(), 20, 50);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setCaretPosition(0);
                textArea.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(textArea);
                JOptionPane.showMessageDialog(this, scrollPane, "Your Info", JOptionPane.INFORMATION_MESSAGE);
            });

            // === Request Aid Logic ===
            btnRequest.addActionListener(e -> {
                try {
                    String amtStr = JOptionPane.showInputDialog(this, "Enter Aid Amount:");
                    if (amtStr == null)
                        return;
                    double amt = Double.parseDouble(amtStr.trim());

                    String reason = JOptionPane.showInputDialog(this, "Enter Reason:");
                    if (reason == null)
                        return;

                    boolean ok = business.submitAidRequest(username, amt, reason);
                    JOptionPane.showMessageDialog(this, ok ? "✅ Aid request submitted!" : "❌ Request failed.");

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "❌ Invalid input.");
                }
            });

            setDefaultCloseOperation(EXIT_ON_CLOSE);
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }

    public static void main(String[] args) {
        new Group6PL();
    }
}
