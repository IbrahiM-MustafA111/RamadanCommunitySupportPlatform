-- ========================================
-- PROCEDURE: show_table
-- Displays all rows from a specified table
-- ========================================
DELIMITER //

CREATE PROCEDURE show_table(IN tbl_name VARCHAR(64))
BEGIN
    SET @query = CONCAT('SELECT * FROM ', tbl_name);
    PREPARE stmt FROM @query;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //

-- ==============================================
-- PROCEDURE: transfer_credits_by_account
-- Transfers credits between two account IDs
-- ==============================================
CREATE PROCEDURE transfer_credits_by_account(
    IN from_account_id INT,
    IN to_account_id INT,
    IN transfer_amount DECIMAL(10,2)
)
BEGIN
    DECLARE from_balance DECIMAL(10,2);
    DECLARE insufficient_balance CONDITION FOR SQLSTATE '45000';

    START TRANSACTION;

    -- Validate account existence
    IF NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = from_account_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sender account does not exist';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = to_account_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Recipient account does not exist';
    END IF;

    -- Check sender balance
    SELECT balance INTO from_balance FROM accounts WHERE account_id = from_account_id;

    IF from_balance < transfer_amount THEN
        ROLLBACK;
        SIGNAL insufficient_balance SET MESSAGE_TEXT = 'Insufficient funds in source account';
    END IF;

    -- Perform the transfer
    UPDATE accounts SET balance = balance - transfer_amount WHERE account_id = from_account_id;
    UPDATE accounts SET balance = balance + transfer_amount WHERE account_id = to_account_id;

    -- Log the transfer
    INSERT INTO transfers (from_account, to_account, amount)
    VALUES (from_account_id, to_account_id, transfer_amount);

    COMMIT;
END //

DELIMITER ;
