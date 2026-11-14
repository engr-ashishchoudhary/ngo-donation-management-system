-- =============================================================================
-- FILE: 02_create_tables.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Create all five tables with constraints, keys, and defaults
-- DEPENDS ON: 01_create_database.sql (database must already exist)
-- EXECUTE: Run after 01_create_database.sql
-- =============================================================================

USE ngo_donation_db;

-- Ensure clean execution order by disabling FK checks temporarily
-- during table creation (tables are created in dependency order anyway,
-- but this guards against accidental re-runs).
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- TABLE 1: admins
-- Stores NGO staff accounts. Referenced by campaigns and expenses.
-- =============================================================================
CREATE TABLE IF NOT EXISTS admins (
    admin_id      INT            NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)   NOT NULL,
    email         VARCHAR(150)   NOT NULL,
    password_hash VARCHAR(255)   NOT NULL COMMENT 'bcrypt hash, never plain text',
    role          ENUM('SUPER_ADMIN', 'ADMIN')
                                 NOT NULL DEFAULT 'ADMIN',
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE
                                 COMMENT 'FALSE = soft-disabled, not deleted',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT pk_admins        PRIMARY KEY (admin_id),
    CONSTRAINT uq_admins_email  UNIQUE      (email)
)
ENGINE = InnoDB
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
COMMENT = 'NGO staff who create campaigns and record expenses';


-- =============================================================================
-- TABLE 2: donors
-- Stores registered donor accounts. Referenced by donations.
-- =============================================================================
CREATE TABLE IF NOT EXISTS donors (
    donor_id      INT            NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)   NOT NULL,
    email         VARCHAR(150)   NOT NULL,
    password_hash VARCHAR(255)   NOT NULL COMMENT 'bcrypt hash, never plain text',
    phone         VARCHAR(15)    NULL     DEFAULT NULL,
    address       TEXT           NULL     DEFAULT NULL,
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE
                                 COMMENT 'FALSE = soft-disabled, not deleted',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT pk_donors        PRIMARY KEY (donor_id),
    CONSTRAINT uq_donors_email  UNIQUE      (email)
)
ENGINE = InnoDB
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
COMMENT = 'Registered donors who make donations to campaigns';


-- =============================================================================
-- TABLE 3: campaigns
-- Fundraising campaigns created by admins.
-- total_collected, total_spent, and available_funds are NOT stored here —
-- they are computed dynamically via views and queries using COALESCE(SUM(), 0).
-- =============================================================================
CREATE TABLE IF NOT EXISTS campaigns (
    campaign_id  INT             NOT NULL AUTO_INCREMENT,
    admin_id     INT             NOT NULL COMMENT 'Admin who created this campaign',
    name         VARCHAR(200)    NOT NULL,
    description  TEXT            NULL     DEFAULT NULL,
    goal_amount  DECIMAL(12, 2)  NOT NULL,
    status       ENUM('ACTIVE', 'CLOSED', 'COMPLETED')
                                 NOT NULL DEFAULT 'ACTIVE',
    start_date   DATE            NOT NULL,
    end_date     DATE            NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT pk_campaigns          PRIMARY KEY (campaign_id),
    CONSTRAINT uq_campaigns_name     UNIQUE      (name),
    CONSTRAINT fk_campaigns_admin    FOREIGN KEY (admin_id)
        REFERENCES admins (admin_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- CHECK constraints (fully enforced in MySQL 8.0.16+)
    CONSTRAINT chk_campaigns_goal_positive
        CHECK (goal_amount > 0),
    CONSTRAINT chk_campaigns_dates
        CHECK (end_date > start_date)
)
ENGINE = InnoDB
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
COMMENT = 'Fundraising campaigns. Financial totals are computed dynamically.';


-- =============================================================================
-- TABLE 4: donations
-- Individual monetary contributions from donors to campaigns.
-- BUSINESS RULE: Only status = COMPLETED rows count toward collected funds.
-- Records are NEVER hard-deleted (immutable financial history).
-- =============================================================================
CREATE TABLE IF NOT EXISTS donations (
    donation_id     INT             NOT NULL AUTO_INCREMENT,
    donor_id        INT             NOT NULL,
    campaign_id     INT             NOT NULL,
    amount          DECIMAL(12, 2)  NOT NULL,
    payment_method  ENUM('CASH', 'BANK_TRANSFER', 'UPI', 'CARD', 'CHEQUE')
                                    NOT NULL,
    status          ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')
                                    NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(100)    NULL     DEFAULT NULL
                                    COMMENT 'External payment gateway reference',
    notes           TEXT            NULL     DEFAULT NULL,
    donated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT pk_donations               PRIMARY KEY (donation_id),
    CONSTRAINT uq_donations_txn_ref       UNIQUE      (transaction_ref),
    CONSTRAINT fk_donations_donor         FOREIGN KEY (donor_id)
        REFERENCES donors (donor_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_donations_campaign      FOREIGN KEY (campaign_id)
        REFERENCES campaigns (campaign_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- CHECK constraint: amount must be positive (BR-01)
    CONSTRAINT chk_donations_amount_positive
        CHECK (amount > 0)
)
ENGINE = InnoDB
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
COMMENT = 'Donor contributions. Only COMPLETED rows count toward campaign funds.';


-- =============================================================================
-- TABLE 5: expenses
-- Expenditures recorded by admins and charged against a campaign.
-- BUSINESS RULE: Amount must not exceed campaign available_funds.
--   This is enforced in the Service layer (pessimistic lock) AND
--   reinforced by the stored procedure sp_record_expense.
-- Records are NEVER hard-deleted (immutable audit trail).
-- =============================================================================
CREATE TABLE IF NOT EXISTS expenses (
    expense_id   INT             NOT NULL AUTO_INCREMENT,
    campaign_id  INT             NOT NULL,
    admin_id     INT             NOT NULL COMMENT 'Admin who recorded the expense',
    description  VARCHAR(255)    NOT NULL,
    amount       DECIMAL(12, 2)  NOT NULL,
    category     ENUM('OPERATIONS', 'LOGISTICS', 'MARKETING',
                      'SALARIES', 'SUPPLIES', 'OTHER')
                                 NOT NULL,
    expense_date DATE            NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT pk_expenses             PRIMARY KEY (expense_id),
    CONSTRAINT fk_expenses_campaign    FOREIGN KEY (campaign_id)
        REFERENCES campaigns (campaign_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_admin       FOREIGN KEY (admin_id)
        REFERENCES admins (admin_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- CHECK constraint: amount must be positive (BR-07)
    CONSTRAINT chk_expenses_amount_positive
        CHECK (amount > 0)
)
ENGINE = InnoDB
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
COMMENT = 'Campaign expenditures. Amount must not exceed available campaign funds.';

-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS = 1;

-- Confirm table creation
SELECT
    table_name,
    table_rows,
    table_comment
FROM information_schema.tables
WHERE table_schema = 'ngo_donation_db'
ORDER BY table_name;
