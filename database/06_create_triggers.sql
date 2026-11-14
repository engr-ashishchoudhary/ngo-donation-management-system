-- =============================================================================
-- FILE: 06_create_triggers.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Database-level triggers for audit and business-rule enforcement
-- DEPENDS ON: 02_create_tables.sql
-- =============================================================================
-- TRIGGER PHILOSOPHY:
--   Triggers complement stored procedures and the Spring Boot Service layer.
--   They provide a final safety net at the database level for the most critical
--   rules that must NEVER be violated, regardless of which client writes to the DB.
--
-- TRIGGERS IMPLEMENTED:
--   1. trg_donations_before_insert   – Enforce campaign ACTIVE check (BR-04)
--   2. trg_donations_before_update   – Enforce status state machine (BR-13)
--   3. trg_donations_prevent_delete  – Prevent hard delete (BR-10 / NFR-06)
--   4. trg_expenses_before_insert    – Enforce available funds check (BR-06)
--   5. trg_expenses_prevent_delete   – Prevent hard delete (BR-11 / NFR-06)
--   6. trg_expenses_prevent_update   – Expenses are immutable after insert (BR-11)
--   7. trg_campaigns_before_update   – Enforce terminal status rule (BR-14)
-- =============================================================================

USE ngo_donation_db;

DELIMITER $$

-- =============================================================================
-- TRIGGER 1: trg_donations_before_insert
-- PURPOSE: Ensure a donation can only be made to an ACTIVE campaign (BR-04)
-- =============================================================================
CREATE TRIGGER trg_donations_before_insert
BEFORE INSERT ON donations
FOR EACH ROW
BEGIN
    DECLARE v_campaign_status VARCHAR(20);

    SELECT status INTO v_campaign_status
    FROM campaigns
    WHERE campaign_id = NEW.campaign_id;

    IF v_campaign_status != 'ACTIVE' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Donations can only be made to ACTIVE campaigns.';
    END IF;
END$$


-- =============================================================================
-- TRIGGER 2: trg_donations_before_update
-- PURPOSE: Enforce the allowed donation status transition state machine (BR-13)
--   PENDING   → COMPLETED  ✅
--   PENDING   → FAILED     ✅
--   COMPLETED → REFUNDED   ✅
--   Any other transition   ❌
-- This trigger is a last-resort database-level guard. The Service layer
-- (sp_update_donation_status) should be the primary enforcement point.
-- =============================================================================
CREATE TRIGGER trg_donations_before_update
BEFORE UPDATE ON donations
FOR EACH ROW
BEGIN
    -- Only fire when the status column is actually changing
    IF OLD.status != NEW.status THEN
        IF NOT (
            (OLD.status = 'PENDING'   AND NEW.status = 'COMPLETED') OR
            (OLD.status = 'PENDING'   AND NEW.status = 'FAILED')    OR
            (OLD.status = 'COMPLETED' AND NEW.status = 'REFUNDED')
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Invalid donation status transition. Allowed: PENDING→COMPLETED, PENDING→FAILED, COMPLETED→REFUNDED.';
        END IF;
    END IF;
END$$


-- =============================================================================
-- TRIGGER 3: trg_donations_prevent_delete
-- PURPOSE: Prevent hard-deletion of donation records (BR-10 / NFR-06)
-- Donation history is immutable for financial integrity.
-- =============================================================================
CREATE TRIGGER trg_donations_prevent_delete
BEFORE DELETE ON donations
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT =
        'Deletion of donation records is not permitted. Financial history is immutable.';
END$$


-- =============================================================================
-- TRIGGER 4: trg_expenses_before_insert
-- PURPOSE: Last-resort check that the new expense does not exceed available
--          campaign funds (BR-06).
-- NOTE: The stored procedure sp_record_expense (with FOR UPDATE lock) is the
--       primary enforcement mechanism. This trigger provides an additional
--       database-level safety net for direct SQL clients.
-- =============================================================================
CREATE TRIGGER trg_expenses_before_insert
BEFORE INSERT ON expenses
FOR EACH ROW
BEGIN
    DECLARE v_total_collected DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_total_spent     DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_available_funds DECIMAL(12,2) DEFAULT 0.00;

    -- Compute available funds (COALESCE guards against NULL from empty SUM)
    SELECT COALESCE(SUM(amount), 0.00)
    INTO   v_total_collected
    FROM   donations
    WHERE  campaign_id = NEW.campaign_id
      AND  status = 'COMPLETED';

    SELECT COALESCE(SUM(amount), 0.00)
    INTO   v_total_spent
    FROM   expenses
    WHERE  campaign_id = NEW.campaign_id;

    SET v_available_funds = v_total_collected - v_total_spent;

    IF NEW.amount > v_available_funds THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Expense amount exceeds available campaign funds. Record rejected.';
    END IF;
END$$


-- =============================================================================
-- TRIGGER 5: trg_expenses_prevent_delete
-- PURPOSE: Prevent hard-deletion of expense records (BR-11 / NFR-06)
-- Expense audit trail is immutable.
-- =============================================================================
CREATE TRIGGER trg_expenses_prevent_delete
BEFORE DELETE ON expenses
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT =
        'Deletion of expense records is not permitted. Audit trail is immutable.';
END$$


-- =============================================================================
-- TRIGGER 6: trg_expenses_prevent_update
-- PURPOSE: Prevent modification of expense records after insertion (BR-11)
-- Once recorded, an expense is a permanent financial fact.
-- =============================================================================
CREATE TRIGGER trg_expenses_prevent_update
BEFORE UPDATE ON expenses
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT =
        'Modification of expense records is not permitted. Expenses are immutable after creation.';
END$$


-- =============================================================================
-- TRIGGER 7: trg_campaigns_before_update
-- PURPOSE: Enforce terminal campaign status rule (BR-14 / FR-10a)
--   CLOSED    → anything  ❌  (terminal)
--   COMPLETED → anything  ❌  (terminal)
--   ACTIVE    → CLOSED    ✅
--   ACTIVE    → COMPLETED ✅
-- =============================================================================
CREATE TRIGGER trg_campaigns_before_update
BEFORE UPDATE ON campaigns
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        -- Block any change away from a terminal state
        IF OLD.status IN ('CLOSED', 'COMPLETED') THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Campaign status cannot be changed: CLOSED and COMPLETED are terminal states.';
        END IF;

        -- Block invalid transitions from ACTIVE
        IF OLD.status = 'ACTIVE' AND NEW.status NOT IN ('CLOSED', 'COMPLETED') THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Invalid campaign status transition. ACTIVE campaigns can only move to CLOSED or COMPLETED.';
        END IF;
    END IF;
END$$


DELIMITER ;

-- Confirm triggers created
SELECT trigger_name, event_manipulation, event_object_table, action_timing
FROM information_schema.triggers
WHERE trigger_schema = 'ngo_donation_db'
ORDER BY event_object_table, action_timing, trigger_name;
