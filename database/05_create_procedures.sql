-- =============================================================================
-- FILE: 05_create_procedures.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Stored procedures for the most critical financial operations
-- DEPENDS ON: 02_create_tables.sql, 04_create_views.sql
-- =============================================================================
-- DESIGN NOTE ON CONCURRENCY (BR-16):
--   sp_record_expense uses SELECT ... FOR UPDATE to acquire a pessimistic row
--   lock on the campaigns row before computing available_funds. This prevents
--   two simultaneous admin requests from both passing the funds check and
--   together overspending the campaign budget.
-- =============================================================================

USE ngo_donation_db;

DELIMITER $$

-- =============================================================================
-- PROCEDURE 1: sp_record_expense
-- Safely inserts a new expense inside a transaction with a pessimistic lock.
-- Called by: Spring Boot ExpenseService.recordExpense()
--
-- Parameters:
--   p_campaign_id  INT           Campaign to charge
--   p_admin_id     INT           Admin recording the expense
--   p_description  VARCHAR(255)  What was spent on
--   p_amount       DECIMAL(12,2) Amount (must be > 0)
--   p_category     VARCHAR(20)   ENUM value
--   p_expense_date DATE          Date incurred
--
-- OUT:
--   p_expense_id   INT           ID of the new expense row (0 on failure)
--   p_message      VARCHAR(500)  Human-readable success/error message
-- =============================================================================
CREATE PROCEDURE sp_record_expense(
    IN  p_campaign_id  INT,
    IN  p_admin_id     INT,
    IN  p_description  VARCHAR(255),
    IN  p_amount       DECIMAL(12, 2),
    IN  p_category     VARCHAR(20),
    IN  p_expense_date DATE,
    OUT p_expense_id   INT,
    OUT p_message      VARCHAR(500)
)
expense_proc: BEGIN
    -- Local variable declarations (must come before any other statements)
    DECLARE v_campaign_status   VARCHAR(20)   DEFAULT NULL;
    DECLARE v_campaign_start    DATE          DEFAULT NULL;
    DECLARE v_total_collected   DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_total_spent       DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_available_funds   DECIMAL(12,2) DEFAULT 0.00;

    -- Initialise OUT parameters
    SET p_expense_id = 0;
    SET p_message    = '';

    -- -------------------------------------------------------------------------
    -- Step 1: Validate input amount
    -- -------------------------------------------------------------------------
    IF p_amount <= 0 THEN
        SET p_message = 'ERROR: Expense amount must be greater than 0.';
        LEAVE expense_proc;
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 2: Begin transaction + acquire pessimistic lock (BR-16)
    -- The FOR UPDATE lock on the campaigns row prevents another concurrent
    -- session from inserting an expense for the same campaign until this
    -- transaction commits or rolls back.
    -- -------------------------------------------------------------------------
    START TRANSACTION;

    SELECT
        c.status,
        c.start_date
    INTO
        v_campaign_status,
        v_campaign_start
    FROM campaigns c
    WHERE c.campaign_id = p_campaign_id
    FOR UPDATE;      -- <-- Pessimistic lock acquired here

    -- -------------------------------------------------------------------------
    -- Step 3: Validate campaign exists
    -- -------------------------------------------------------------------------
    IF v_campaign_status IS NULL THEN
        ROLLBACK;
        SET p_message = CONCAT('ERROR: Campaign ID ', p_campaign_id, ' not found.');
        LEAVE expense_proc;
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 4: Validate expense_date >= campaign start_date (BR-15)
    -- -------------------------------------------------------------------------
    IF p_expense_date < v_campaign_start THEN
        ROLLBACK;
        SET p_message = CONCAT(
            'ERROR: Expense date (', p_expense_date,
            ') cannot be before the campaign start date (', v_campaign_start, ').'
        );
        LEAVE expense_proc;
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 5: Compute available funds while holding the lock (BR-06)
    -- COALESCE ensures 0.00 when no rows exist, never NULL.
    -- -------------------------------------------------------------------------
    SELECT
        COALESCE(SUM(d.amount), 0.00)
    INTO v_total_collected
    FROM donations d
    WHERE d.campaign_id = p_campaign_id
      AND d.status = 'COMPLETED';

    SELECT
        COALESCE(SUM(e.amount), 0.00)
    INTO v_total_spent
    FROM expenses e
    WHERE e.campaign_id = p_campaign_id;

    SET v_available_funds = v_total_collected - v_total_spent;

    -- -------------------------------------------------------------------------
    -- Step 6: Reject if expense would exceed available funds (BR-06)
    -- -------------------------------------------------------------------------
    IF p_amount > v_available_funds THEN
        ROLLBACK;
        SET p_message = CONCAT(
            'ERROR: Expense amount (', FORMAT(p_amount, 2),
            ') exceeds available campaign funds (', FORMAT(v_available_funds, 2), ').'
        );
        LEAVE expense_proc;
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 7: Insert the expense
    -- -------------------------------------------------------------------------
    INSERT INTO expenses (campaign_id, admin_id, description, amount, category, expense_date)
    VALUES (p_campaign_id, p_admin_id, p_description, p_amount, p_category, p_expense_date);

    SET p_expense_id = LAST_INSERT_ID();

    COMMIT;

    SET p_message = CONCAT(
        'SUCCESS: Expense ID ', p_expense_id,
        ' recorded. Available funds remaining: ',
        FORMAT(v_available_funds - p_amount, 2), '.'
    );
END expense_proc$$


-- =============================================================================
-- PROCEDURE 2: sp_update_donation_status
-- Updates a donation status enforcing the allowed state machine (FR-18a, BR-13):
--   PENDING → COMPLETED
--   PENDING → FAILED
--   COMPLETED → REFUNDED
-- All other transitions are rejected.
--
-- Parameters:
--   p_donation_id  INT          Target donation
--   p_new_status   VARCHAR(20)  New ENUM status value
--   p_admin_id     INT          Admin performing the update (for audit)
--
-- OUT:
--   p_message      VARCHAR(500) Success or error description
-- =============================================================================
CREATE PROCEDURE sp_update_donation_status(
    IN  p_donation_id INT,
    IN  p_new_status  VARCHAR(20),
    IN  p_admin_id    INT,
    OUT p_message     VARCHAR(500)
)
donation_status_proc: BEGIN
    DECLARE v_current_status VARCHAR(20) DEFAULT NULL;

    SET p_message = '';

    -- Fetch current status
    SELECT status INTO v_current_status
    FROM donations
    WHERE donation_id = p_donation_id;

    -- Validate the donation exists
    IF v_current_status IS NULL THEN
        SET p_message = CONCAT('ERROR: Donation ID ', p_donation_id, ' not found.');
        LEAVE donation_status_proc;
    END IF;

    -- Enforce state machine (FR-18a / BR-13)
    IF NOT (
        (v_current_status = 'PENDING'   AND p_new_status = 'COMPLETED') OR
        (v_current_status = 'PENDING'   AND p_new_status = 'FAILED')    OR
        (v_current_status = 'COMPLETED' AND p_new_status = 'REFUNDED')
    ) THEN
        SET p_message = CONCAT(
            'ERROR: Transition from ', v_current_status,
            ' to ', p_new_status, ' is not permitted.'
        );
        LEAVE donation_status_proc;
    END IF;

    -- Apply the update
    UPDATE donations
    SET status = p_new_status
    WHERE donation_id = p_donation_id;

    SET p_message = CONCAT(
        'SUCCESS: Donation ID ', p_donation_id,
        ' status changed from ', v_current_status,
        ' to ', p_new_status, '.'
    );
END donation_status_proc$$


-- =============================================================================
-- PROCEDURE 3: sp_update_campaign_status
-- Updates a campaign status enforcing terminal-state rules (FR-10a / BR-14):
--   ACTIVE → CLOSED
--   ACTIVE → COMPLETED
-- CLOSED and COMPLETED are terminal — cannot change back to ACTIVE.
--
-- Parameters:
--   p_campaign_id  INT          Target campaign
--   p_new_status   VARCHAR(20)  New ENUM status value
--   p_admin_id     INT          Admin performing the action
--
-- OUT:
--   p_message      VARCHAR(500) Success or error description
-- =============================================================================
CREATE PROCEDURE sp_update_campaign_status(
    IN  p_campaign_id INT,
    IN  p_new_status  VARCHAR(20),
    IN  p_admin_id    INT,
    OUT p_message     VARCHAR(500)
)
campaign_status_proc: BEGIN
    DECLARE v_current_status VARCHAR(20) DEFAULT NULL;

    SET p_message = '';

    SELECT status INTO v_current_status
    FROM campaigns
    WHERE campaign_id = p_campaign_id;

    IF v_current_status IS NULL THEN
        SET p_message = CONCAT('ERROR: Campaign ID ', p_campaign_id, ' not found.');
        LEAVE campaign_status_proc;
    END IF;

    -- Terminal-state enforcement (BR-14): only ACTIVE campaigns can change status
    IF v_current_status != 'ACTIVE' THEN
        SET p_message = CONCAT(
            'ERROR: Campaign is already in terminal state ',
            v_current_status, ' and cannot be changed.'
        );
        LEAVE campaign_status_proc;
    END IF;

    -- Validate target status
    IF p_new_status NOT IN ('CLOSED', 'COMPLETED') THEN
        SET p_message = CONCAT(
            'ERROR: Target status must be CLOSED or COMPLETED, not "', p_new_status, '".'
        );
        LEAVE campaign_status_proc;
    END IF;

    UPDATE campaigns
    SET status = p_new_status
    WHERE campaign_id = p_campaign_id;

    SET p_message = CONCAT(
        'SUCCESS: Campaign ID ', p_campaign_id,
        ' status changed from ACTIVE to ', p_new_status, '.'
    );
END campaign_status_proc$$


-- =============================================================================
-- PROCEDURE 4: sp_get_campaign_financials
-- Returns the financial summary for a single campaign.
-- Uses COALESCE(SUM(), 0.00) consistently (Phase 1 requirement).
--
-- Parameters:
--   p_campaign_id  INT  Campaign to summarise
-- =============================================================================
CREATE PROCEDURE sp_get_campaign_financials(
    IN p_campaign_id INT
)
BEGIN
    SELECT
        c.campaign_id,
        c.name                                               AS campaign_name,
        c.goal_amount,
        c.status,
        c.start_date,
        c.end_date,

        COALESCE(
            (SELECT SUM(d.amount)
             FROM donations d
             WHERE d.campaign_id = c.campaign_id
               AND d.status = 'COMPLETED'),
            0.00
        )                                                    AS total_collected,

        COALESCE(
            (SELECT SUM(e.amount)
             FROM expenses e
             WHERE e.campaign_id = c.campaign_id),
            0.00
        )                                                    AS total_spent,

        COALESCE(
            (SELECT SUM(d.amount)
             FROM donations d
             WHERE d.campaign_id = c.campaign_id
               AND d.status = 'COMPLETED'),
            0.00
        ) -
        COALESCE(
            (SELECT SUM(e.amount)
             FROM expenses e
             WHERE e.campaign_id = c.campaign_id),
            0.00
        )                                                    AS available_funds

    FROM campaigns c
    WHERE c.campaign_id = p_campaign_id;
END$$


DELIMITER ;

-- Confirm procedures created
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'ngo_donation_db'
  AND routine_type   = 'PROCEDURE'
ORDER BY routine_name;
