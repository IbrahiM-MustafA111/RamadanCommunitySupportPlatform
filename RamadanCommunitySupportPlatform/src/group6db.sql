-- Group6DB.SQL
-- ISTE-330 Group 6 | Ibraheem Mustafa

-- 1. Drop and Create Database
DROP DATABASE IF EXISTS group6db;
CREATE DATABASE group6db
    DEFAULT CHARACTER SET utf8
    DEFAULT COLLATE utf8_general_ci;
USE group6db;

-- 2. Users Table (Admin + Clients)
CREATE TABLE users (
    user_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password CHAR(40) NOT NULL,  -- SHA-1 hash
    role ENUM('admin', 'client') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 3. Accounts Table (stores client funds/credits)
CREATE TABLE accounts (
    account_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    account_type VARCHAR(50),
    balance DECIMAL(10,2) UNSIGNED DEFAULT 0.00,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 4. Transfers Table (track credit transfers)
CREATE TABLE transfers (
    transfer_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    from_account INT UNSIGNED NOT NULL,
    to_account INT UNSIGNED NOT NULL,
    amount DECIMAL(10,2) UNSIGNED NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_account) REFERENCES accounts(account_id),
    FOREIGN KEY (to_account) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 5. Requests Table (for community support credits)
CREATE TABLE requests (
    request_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    amount DECIMAL(10,2) UNSIGNED NOT NULL,
    reason VARCHAR(255),
    status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 6. Admin Logs Table (optional - for tracking admin actions)
CREATE TABLE admin_logs (
    log_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    admin_id INT UNSIGNED NOT NULL,
    action VARCHAR(255),
    log_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 7. Sample Users (hashed passwords)
INSERT INTO users (username, password, role) VALUES
('admin1', 'a123', 'admin'),
('client1', 'c123', 'client'),
('client2', 'da4b9237bacccdf19c0760cab7aec4a8359010b0', 'client');

-- 8. Sample Accounts
INSERT INTO accounts (user_id, account_type, balance) VALUES
(2, 'Ramadan Zakat', 100.00),
(3, 'Charity Credits', 150.00);

-- 9. Sample Requests
INSERT INTO requests (user_id, amount, reason, status) VALUES
(2, 25.00, 'Medical support', 'pending'),
(3, 50.00, 'Family food basket', 'approved');

-- 10. Sample Admin Logs
INSERT INTO admin_logs (admin_id, action) VALUES
(1, 'Viewed all client accounts');
