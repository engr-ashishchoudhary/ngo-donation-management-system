-- =============================================================================
-- FILE: 03_create_indexes.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Create additional performance indexes beyond primary/unique keys
-- DEPENDS ON: 02_create_tables.sql
-- =============================================================================
-- NOTE: MySQL automatically creates indexes for PRIMARY KEY and UNIQUE
-- constraints. The indexes below target the most frequent WHERE, JOIN,
-- ORDER BY and GROUP BY patterns expected from the Spring Boot application.
-- =============================================================================

USE ngo_donation_db;

-- ---------------------------------------------------------------------------
-- TABLE: admins
-- ---------------------------------------------------------------------------
-- Lookup active admins quickly (dashboard, auth filters)
CREATE INDEX idx_admins_is_active
    ON admins (is_active);

-- Lookup by role (SUPER_ADMIN management page)
CREATE INDEX idx_admins_role
    ON admins (role);


-- ---------------------------------------------------------------------------
-- TABLE: donors
-- ---------------------------------------------------------------------------
-- Lookup active donors quickly
CREATE INDEX idx_donors_is_active
    ON donors (is_active);


-- ---------------------------------------------------------------------------
-- TABLE: campaigns
-- ---------------------------------------------------------------------------
-- Most common filter: list ACTIVE campaigns for the public campaign page
CREATE INDEX idx_campaigns_status
    ON campaigns (status);

-- Filter campaigns by admin (admin's own campaign list)
CREATE INDEX idx_campaigns_admin_id
    ON campaigns (admin_id);

-- Date-range queries and ordering by start/end date
CREATE INDEX idx_campaigns_start_date
    ON campaigns (start_date);

CREATE INDEX idx_campaigns_end_date
    ON campaigns (end_date);


-- ---------------------------------------------------------------------------
-- TABLE: donations
-- ---------------------------------------------------------------------------
-- Core financial aggregate: SUM of COMPLETED donations per campaign
-- This index is critical for view vw_campaign_financials performance.
CREATE INDEX idx_donations_campaign_status
    ON donations (campaign_id, status);

-- Donor history page: list all donations by one donor
CREATE INDEX idx_donations_donor_id
    ON donations (donor_id);

-- Admin filter: donations ordered by time
CREATE INDEX idx_donations_donated_at
    ON donations (donated_at DESC);

-- Admin filter by status (e.g. all PENDING donations)
CREATE INDEX idx_donations_status
    ON donations (status);


-- ---------------------------------------------------------------------------
-- TABLE: expenses
-- ---------------------------------------------------------------------------
-- Core financial aggregate: SUM of all expenses per campaign
-- Also critical for vw_campaign_financials.
CREATE INDEX idx_expenses_campaign_id
    ON expenses (campaign_id);

-- Admin filter: expenses recorded by a specific admin
CREATE INDEX idx_expenses_admin_id
    ON expenses (admin_id);

-- Date-range filter on expense_date
CREATE INDEX idx_expenses_expense_date
    ON expenses (expense_date);

-- Filter by category (reporting)
CREATE INDEX idx_expenses_category
    ON expenses (category);

-- Confirm all indexes
SELECT
    table_name,
    index_name,
    column_name,
    non_unique
FROM information_schema.statistics
WHERE table_schema = 'ngo_donation_db'
ORDER BY table_name, index_name, seq_in_index;
