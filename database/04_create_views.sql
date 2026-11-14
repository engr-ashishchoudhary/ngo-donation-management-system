-- =============================================================================
-- FILE: 04_create_views.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Create reusable views for financial summaries and reporting
-- DEPENDS ON: 02_create_tables.sql, 03_create_indexes.sql
-- =============================================================================
-- DESIGN NOTE:
--   All monetary totals (total_collected, total_spent, available_funds) are
--   computed here using COALESCE(SUM(), 0.00) so the result is always a
--   numeric DECIMAL value, never NULL — even for new campaigns with no
--   donations or expenses yet. (Approved in Phase 1 database design.)
-- =============================================================================

USE ngo_donation_db;

-- =============================================================================
-- VIEW 1: vw_campaign_financials
-- Full financial summary for every campaign.
-- Used by: campaign list page, campaign detail page, admin dashboard.
-- =============================================================================
CREATE OR REPLACE VIEW vw_campaign_financials AS
SELECT
    c.campaign_id,
    c.name                                           AS campaign_name,
    c.status,
    c.goal_amount,
    c.start_date,
    c.end_date,
    a.admin_id                                       AS created_by_admin_id,
    a.name                                           AS created_by_admin_name,

    -- Total collected = SUM of COMPLETED donations only (BR-05)
    COALESCE(
        (SELECT SUM(d.amount)
         FROM donations d
         WHERE d.campaign_id = c.campaign_id
           AND d.status = 'COMPLETED'),
        0.00
    )                                                AS total_collected,

    -- Total spent = SUM of all expenses against this campaign
    COALESCE(
        (SELECT SUM(e.amount)
         FROM expenses e
         WHERE e.campaign_id = c.campaign_id),
        0.00
    )                                                AS total_spent,

    -- Available funds = collected − spent
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
    )                                                AS available_funds,

    -- Progress percentage toward goal (capped at 100% for display)
    LEAST(
        ROUND(
            COALESCE(
                (SELECT SUM(d.amount)
                 FROM donations d
                 WHERE d.campaign_id = c.campaign_id
                   AND d.status = 'COMPLETED'),
                0.00
            ) / c.goal_amount * 100,
            2
        ),
        100.00
    )                                                AS progress_percent,

    -- Donor count for this campaign
    (SELECT COUNT(DISTINCT d.donor_id)
     FROM donations d
     WHERE d.campaign_id = c.campaign_id
       AND d.status = 'COMPLETED')                   AS unique_donors_count,

    -- Donation count breakdown
    (SELECT COUNT(*) FROM donations d
     WHERE d.campaign_id = c.campaign_id)            AS total_donations_count,

    (SELECT COUNT(*) FROM donations d
     WHERE d.campaign_id = c.campaign_id
       AND d.status = 'COMPLETED')                   AS completed_donations_count,

    (SELECT COUNT(*) FROM donations d
     WHERE d.campaign_id = c.campaign_id
       AND d.status = 'PENDING')                     AS pending_donations_count,

    c.created_at,
    c.updated_at

FROM campaigns c
JOIN admins a ON c.admin_id = a.admin_id;


-- =============================================================================
-- VIEW 2: vw_donation_details
-- Enriched donation rows joining donor and campaign names.
-- Used by: admin donation list, donor "my donations" page.
-- =============================================================================
CREATE OR REPLACE VIEW vw_donation_details AS
SELECT
    don.donation_id,
    don.amount,
    don.payment_method,
    don.status,
    don.transaction_ref,
    don.notes,
    don.donated_at,

    -- Donor info
    dr.donor_id,
    dr.name   AS donor_name,
    dr.email  AS donor_email,

    -- Campaign info
    c.campaign_id,
    c.name    AS campaign_name,
    c.status  AS campaign_status

FROM donations don
JOIN donors    dr ON don.donor_id    = dr.donor_id
JOIN campaigns c  ON don.campaign_id = c.campaign_id;


-- =============================================================================
-- VIEW 3: vw_expense_details
-- Enriched expense rows joining campaign and admin names.
-- Used by: admin expense list per campaign.
-- =============================================================================
CREATE OR REPLACE VIEW vw_expense_details AS
SELECT
    ex.expense_id,
    ex.description,
    ex.amount,
    ex.category,
    ex.expense_date,
    ex.created_at,

    -- Campaign info
    c.campaign_id,
    c.name  AS campaign_name,
    c.status AS campaign_status,

    -- Admin info
    a.admin_id,
    a.name  AS admin_name,
    a.role  AS admin_role

FROM expenses  ex
JOIN campaigns c ON ex.campaign_id = c.campaign_id
JOIN admins    a ON ex.admin_id    = a.admin_id;


-- =============================================================================
-- VIEW 4: vw_admin_dashboard
-- Single-row dashboard metrics for the admin home page.
-- Used by: GET /api/admin/dashboard
-- =============================================================================
CREATE OR REPLACE VIEW vw_admin_dashboard AS
SELECT
    -- Counts
    (SELECT COUNT(*) FROM admins  WHERE is_active = TRUE)    AS total_active_admins,
    (SELECT COUNT(*) FROM donors  WHERE is_active = TRUE)    AS total_active_donors,
    (SELECT COUNT(*) FROM campaigns)                         AS total_campaigns,
    (SELECT COUNT(*) FROM campaigns WHERE status = 'ACTIVE') AS active_campaigns_count,

    -- Financial totals across the entire NGO
    COALESCE(
        (SELECT SUM(amount) FROM donations WHERE status = 'COMPLETED'),
        0.00
    )                                                        AS grand_total_collected,

    COALESCE(
        (SELECT SUM(amount) FROM expenses),
        0.00
    )                                                        AS grand_total_spent,

    COALESCE(
        (SELECT SUM(amount) FROM donations WHERE status = 'COMPLETED'),
        0.00
    ) -
    COALESCE(
        (SELECT SUM(amount) FROM expenses),
        0.00
    )                                                        AS grand_total_available,

    -- Donation counts by status
    (SELECT COUNT(*) FROM donations WHERE status = 'COMPLETED') AS total_completed_donations,
    (SELECT COUNT(*) FROM donations WHERE status = 'PENDING')   AS total_pending_donations,
    (SELECT COUNT(*) FROM donations WHERE status = 'FAILED')    AS total_failed_donations,
    (SELECT COUNT(*) FROM donations WHERE status = 'REFUNDED')  AS total_refunded_donations;


-- =============================================================================
-- VIEW 5: vw_donor_summary
-- Per-donor giving summary: total donated, number of campaigns supported.
-- Used by: admin donor list, donor profile page.
-- =============================================================================
CREATE OR REPLACE VIEW vw_donor_summary AS
SELECT
    dr.donor_id,
    dr.name,
    dr.email,
    dr.phone,
    dr.is_active,
    dr.created_at,

    COALESCE(SUM(CASE WHEN don.status = 'COMPLETED' THEN don.amount ELSE 0 END), 0.00)
                                            AS total_donated,

    COUNT(CASE WHEN don.status = 'COMPLETED' THEN 1 END)
                                            AS completed_donations_count,

    COUNT(DISTINCT CASE WHEN don.status = 'COMPLETED' THEN don.campaign_id END)
                                            AS campaigns_supported_count

FROM donors dr
LEFT JOIN donations don ON dr.donor_id = don.donor_id
GROUP BY
    dr.donor_id,
    dr.name,
    dr.email,
    dr.phone,
    dr.is_active,
    dr.created_at;


-- Confirm views created
SELECT table_name AS view_name
FROM information_schema.views
WHERE table_schema = 'ngo_donation_db'
ORDER BY table_name;
