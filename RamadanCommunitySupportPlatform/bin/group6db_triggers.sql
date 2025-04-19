-- triggers_group6.sql
-- ISTE-330 Group 6 – Triggers for Ramadan Support Platform

DELIMITER $$

-- 1. Log when a new user is added
CREATE TRIGGER trg_log_user_add
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO admin_logs (admin_id, action)
    VALUES (1, CONCAT('Added new user: ', NEW.username));
END $$

-- 2. Log after a transfer is made
CREATE TRIGGER trg_log_transfer_insert
AFTER INSERT ON transfers
FOR EACH ROW
BEGIN
    INSERT INTO admin_logs (admin_id, action)
    VALUES (1, CONCAT('Transfer from account ', NEW.from_account, ' to ', NEW.to_account, ' of amount ', NEW.amount));
END $$

-- 3. Log when a client submits an aid request
CREATE TRIGGER trg_log_aid_request
AFTER INSERT ON requests
FOR EACH ROW
BEGIN
    INSERT INTO admin_logs (admin_id, action)
    VALUES (1, CONCAT('New aid request submitted by user ID ', NEW.user_id, ' for amount ', NEW.amount));
END $$

-- 4. Log when aid request status is updated
CREATE TRIGGER trg_log_request_status_update
AFTER UPDATE ON requests
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO admin_logs (admin_id, action)
        VALUES (1, CONCAT('Updated request ID ', NEW.request_id, ' status from ', OLD.status, ' to ', NEW.status));
    END IF;
END $$

DELIMITER ;
