-- =============================================================================
-- FILE: 08_test_queries.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Verification and test queries for all Phase 2 requirements
-- DEPENDS ON: 07_insert_sample_data.sql (all tables populated)
-- =============================================================================
-- HOW TO READ RESULTS:
--   Each test section starts with a comment describing what is being tested
--   and what the expected result is. Run each block and compare the output.
-- =============================================================================

USE ngo_donation_db;

-- =============================================================================
-- TEST 1: TABLE CREATION VERIFICATION
-- Expected: 5 tables listed with correct engine and charset
-- =============================================================================
SELECT '=== TEST 1: TABLE CREATION ===' AS test_section;

SELECT
    table_name,
    engine,
    table_collation,
    table_comment
FROM information_schema.tables
WHERE table_schema = 'ngo_donation_db'
  AND table_type   = 'BASE TABLE'
ORDER BY table_name;

-- Expected output: admins, campaigns, donations, donors, expenses
-- Engine: InnoDB | Collation: utf8mb4_unicode_ci


-- =============================================================================
-- TEST 2: PRIMARY AND FOREIGN KEY VERIFICATION
-- Expected: All PKs and FKs from the approved database design
-- =============================================================================
SELECT '=== TEST 2: PRIMARY AND FOREIGN KEYS ===' AS test_section;

-- Primary keys
SELECT
    tc.table_name,
    kcu.column_name,
    tc.constraint_type
FROM information_schema.table_constraints  tc
JOIN information_schema.key_column_usage   kcu
     ON  tc.constraint_name = kcu.constraint_name
     AND tc.table_schema    = kcu.table_schema
     AND tc.table_name      = kcu.table_name
WHERE tc.table_schema    = 'ngo_donation_db'
  AND tc.constraint_type IN ('PRIMARY KEY', 'FOREIGN KEY')
ORDER BY tc.table_name, tc.constraint_type, kcu.column_name;

-- Expected foreign keys:
--   campaigns.admin_id → admins.admin_id
--   donations.donor_id    → donors.donor_id
--   donations.campaign_id → campaigns.campaign_id
--   expenses.campaign_id  → campaigns.campaign_id
--   expenses.admin_id     → admins.admin_id


-- =============================================================================
-- TEST 3: UNIQUE CONSTRAINT — DUPLICATE EMAIL PREVENTION
-- Expected: Both INSERTs below fail with error 1062 (Duplicate entry)
-- =============================================================================
SELECT '=== TEST 3: DUPLICATE EMAIL PREVENTION ===' AS test_section;

-- Test 3a: Duplicate admin email (should FAIL)
-- Uncomment to test manually:
-- INSERT INTO admins (name, email, password_hash, role)
-- VALUES ('Fake Admin', 'priya.sharma@sewngo.org', '$2a$10$fake', 'ADMIN');
-- Expected: ERROR 1062 - Duplicate entry 'priya.sharma@sewngo.org' for key 'uq_admins_email'

-- Test 3b: Duplicate donor email (should FAIL)
-- INSERT INTO donors (name, email, password_hash)
-- VALUES ('Fake Donor', 'arjun.nair@gmail.com', '$2a$10$fake');
-- Expected: ERROR 1062 - Duplicate entry

-- Verify unique emails currently exist (no duplicates)
SELECT 'admins_email_uniqueness' AS check_name,
    COUNT(*)                     AS total_admins,
    COUNT(DISTINCT email)        AS unique_emails,
    (COUNT(*) = COUNT(DISTINCT email)) AS is_unique
FROM admins
UNION ALL
SELECT 'donors_email_uniqueness',
    COUNT(*), COUNT(DISTINCT email),
    (COUNT(*) = COUNT(DISTINCT email))
FROM donors;
-- Expected: is_unique = 1 for both rows


-- =============================================================================
-- TEST 4: DONATION STATUS CALCULATION
-- Only COMPLETED donations count toward campaign funds (BR-05)
-- Expected per campaign:
--   Campaign 1 (Swachh Jal): COMPLETED = 105000.00, excluded = 31000.00
--   Campaign 2 (Shiksha Roshni): COMPLETED = 28500.00, excluded = 4000.00
-- =============================================================================
SELECT '=== TEST 4: DONATION STATUS CALCULATIONS ===' AS test_section;

SELECT
    c.campaign_id,
    c.name AS campaign_name,
    don.status,
    COUNT(*)                   AS donation_count,
    SUM(don.amount)            AS status_total,
    ROUND(SUM(don.amount) /
        SUM(SUM(don.amount)) OVER (PARTITION BY c.campaign_id) * 100, 2)
                               AS pct_of_campaign_total
FROM donations don
JOIN campaigns c ON don.campaign_id = c.campaign_id
GROUP BY c.campaign_id, c.name, don.status
ORDER BY c.campaign_id, don.status;


-- =============================================================================
-- TEST 5: CAMPAIGN COLLECTED FUNDS — COALESCE VERIFICATION
-- Expected: COMPLETED-only totals with 0.00 (not NULL) for campaigns
-- without any COMPLETED donations
-- =============================================================================
SELECT '=== TEST 5: CAMPAIGN COLLECTED FUNDS ===' AS test_section;

SELECT
    c.campaign_id,
    c.name,
    c.goal_amount,
    COALESCE(SUM(CASE WHEN d.status = 'COMPLETED' THEN d.amount END), 0.00)
                            AS total_collected,
    ROUND(
        COALESCE(SUM(CASE WHEN d.status = 'COMPLETED' THEN d.amount END), 0.00)
        / c.goal_amount * 100, 2
    )                       AS progress_pct
FROM campaigns c
LEFT JOIN donations d ON c.campaign_id = d.campaign_id
GROUP BY c.campaign_id, c.name, c.goal_amount
ORDER BY c.campaign_id;

-- Expected results:
-- Campaign 1: 105000.00 / 500000.00 = 21.00%
-- Campaign 2:  28500.00 / 200000.00 = 14.25%
-- Campaign 3:      0.00 (no active donations) → shows COALESCE working
-- Campaign 4:      0.00 → shows COALESCE working


-- =============================================================================
-- TEST 6: EXPENSE CALCULATION — AVAILABLE FUNDS
-- =============================================================================
SELECT '=== TEST 6: EXPENSE CALCULATIONS AND AVAILABLE FUNDS ===' AS test_section;

SELECT
    c.campaign_id,
    c.name,
    COALESCE(
        (SELECT SUM(d.amount) FROM donations d
         WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'),
        0.00
    )                           AS total_collected,
    COALESCE(
        (SELECT SUM(e.amount) FROM expenses e
         WHERE e.campaign_id = c.campaign_id),
        0.00
    )                           AS total_spent,
    COALESCE(
        (SELECT SUM(d.amount) FROM donations d
         WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'),
        0.00
    ) -
    COALESCE(
        (SELECT SUM(e.amount) FROM expenses e
         WHERE e.campaign_id = c.campaign_id),
        0.00
    )                           AS available_funds
FROM campaigns c
ORDER BY c.campaign_id;

-- Expected:
-- Campaign 1: collected=105000, spent=60000, available=45000
-- Campaign 2: collected= 28500, spent=19000, available= 9500
-- Campaign 3: collected=     0, spent=    0, available=    0
-- Campaign 4: collected=     0, spent=    0, available=    0


-- =============================================================================
-- TEST 7: OVERSPEND PREVENTION (via trigger + procedure)
-- Expected: Both attempts below are rejected
-- =============================================================================
SELECT '=== TEST 7: OVERSPEND PREVENTION ===' AS test_section;

-- Test 7a: Try to insert an expense larger than available funds for campaign 1
-- Campaign 1 available = 45000.00; attempt 90000.00 → should FAIL
-- Uncomment to test manually:
-- INSERT INTO expenses (campaign_id, admin_id, description, amount, category, expense_date)
-- VALUES (1, 1, 'Test overspend', 90000.00, 'OTHER', '2026-11-14');
-- Expected: ERROR 1644 - Expense amount exceeds available campaign funds.

-- Test 7b: Call procedure with overspend
-- CALL sp_record_expense(1, 1, 'Overspend test', 90000.00, 'OTHER', '2026-11-14', @eid, @msg);
-- SELECT @eid AS expense_id, @msg AS message;
-- Expected: expense_id=0, message starts with 'ERROR: Expense amount...'

-- Confirm current available funds (verify test 6 figures)
SELECT
    c.campaign_id,
    c.name,
    COALESCE((SELECT SUM(d.amount) FROM donations d
              WHERE d.campaign_id = c.campaign_id AND d.status='COMPLETED'), 0.00)
            - COALESCE((SELECT SUM(e.amount) FROM expenses e
              WHERE e.campaign_id = c.campaign_id), 0.00)
              AS available_funds
FROM campaigns c
WHERE c.campaign_id IN (1, 2);


-- =============================================================================
-- TEST 8: JOIN OPERATIONS
-- Rich query joining all five tables
-- =============================================================================
SELECT '=== TEST 8: JOIN QUERIES ===' AS test_section;

-- 8a: Full donation history with donor, campaign, and admin context
SELECT
    don.donation_id,
    dr.name        AS donor_name,
    dr.email       AS donor_email,
    c.name         AS campaign_name,
    c.status       AS campaign_status,
    don.amount,
    don.payment_method,
    don.status     AS donation_status,
    don.donated_at
FROM donations don
JOIN donors    dr ON don.donor_id    = dr.donor_id
JOIN campaigns c  ON don.campaign_id = c.campaign_id
ORDER BY don.donated_at DESC;

-- 8b: Full expense history with campaign and admin context
SELECT
    ex.expense_id,
    c.name  AS campaign_name,
    a.name  AS recorded_by,
    a.role  AS admin_role,
    ex.description,
    ex.amount,
    ex.category,
    ex.expense_date
FROM expenses  ex
JOIN campaigns c ON ex.campaign_id = c.campaign_id
JOIN admins    a ON ex.admin_id    = a.admin_id
ORDER BY ex.expense_date;

-- 8c: Per-donor total giving (LEFT JOIN to include donors with zero donations)
SELECT
    dr.donor_id,
    dr.name,
    dr.email,
    COALESCE(SUM(CASE WHEN don.status = 'COMPLETED' THEN don.amount ELSE 0 END), 0.00)
            AS total_donated,
    COUNT(CASE WHEN don.status = 'COMPLETED' THEN 1 END)
            AS completed_count
FROM donors dr
LEFT JOIN donations don ON dr.donor_id = don.donor_id
GROUP BY dr.donor_id, dr.name, dr.email
ORDER BY total_donated DESC;


-- =============================================================================
-- TEST 9: STORED PROCEDURES
-- =============================================================================
SELECT '=== TEST 9: STORED PROCEDURES ===' AS test_section;

-- 9a: Get financial summary for campaign 1
CALL sp_get_campaign_financials(1);
-- Expected: campaign 1, collected=105000, spent=60000, available=45000

-- 9b: Valid expense (within available funds for campaign 1)
CALL sp_record_expense(1, 1, 'Generator fuel for water pump site', 5000.00, 'OPERATIONS', '2026-11-14', @new_eid, @new_msg);
SELECT @new_eid AS new_expense_id, @new_msg AS procedure_message;
-- Expected: new_expense_id = 7, message = SUCCESS ... remaining = 40000.00

-- Verify new available funds
CALL sp_get_campaign_financials(1);
-- Expected: available_funds = 40000.00 (was 45000, minus 5000)

-- 9c: Overspend attempt via procedure (should return error)
CALL sp_record_expense(1, 1, 'Overspend test', 99999.00, 'OTHER', '2026-11-14', @fail_eid, @fail_msg);
SELECT @fail_eid AS expense_id_on_fail, @fail_msg AS error_message;
-- Expected: expense_id_on_fail = 0, error_message starts with 'ERROR: Expense amount...'

-- 9d: Valid donation status update
CALL sp_update_donation_status(6, 'COMPLETED', 1, @dmsg);
SELECT @dmsg AS donation_status_message;
-- Expected: SUCCESS: Donation ID 6 status changed from PENDING to COMPLETED.

-- 9e: Invalid donation status transition (should fail)
CALL sp_update_donation_status(1, 'PENDING', 1, @dmsg2);
SELECT @dmsg2 AS invalid_transition_message;
-- Expected: ERROR: Transition from COMPLETED to PENDING is not permitted.

-- 9f: Campaign status update to COMPLETED
CALL sp_update_campaign_status(2, 'COMPLETED', 1, @cmsg);
SELECT @cmsg AS campaign_status_message;
-- Expected: SUCCESS: Campaign ID 2 status changed from ACTIVE to COMPLETED.

-- 9g: Attempt to re-open COMPLETED campaign (should fail)
CALL sp_update_campaign_status(2, 'ACTIVE', 1, @cmsg2);
SELECT @cmsg2 AS terminal_state_error;
-- Expected: ERROR: Campaign is already in terminal state COMPLETED...


-- =============================================================================
-- TEST 10: TRIGGERS
-- =============================================================================
SELECT '=== TEST 10: TRIGGERS ===' AS test_section;

-- 10a: Attempt donation to a CLOSED campaign (campaign_id 4)
-- Expected: ERROR from trg_donations_before_insert
-- INSERT INTO donations (donor_id, campaign_id, amount, payment_method, status)
-- VALUES (1, 4, 5000.00, 'UPI', 'PENDING');
-- Expected: ERROR 1644 - Donations can only be made to ACTIVE campaigns.

-- 10b: Attempt invalid donation status transition (COMPLETED → PENDING)
-- Expected: ERROR from trg_donations_before_update
-- UPDATE donations SET status = 'PENDING' WHERE donation_id = 1;
-- Expected: ERROR 1644 - Invalid donation status transition...

-- 10c: Attempt to delete a donation (should ALWAYS fail)
-- Expected: ERROR from trg_donations_prevent_delete
-- DELETE FROM donations WHERE donation_id = 7;
-- Expected: ERROR 1644 - Deletion of donation records is not permitted...

-- 10d: Attempt to delete an expense (should ALWAYS fail)
-- Expected: ERROR from trg_expenses_prevent_delete
-- DELETE FROM expenses WHERE expense_id = 1;
-- Expected: ERROR 1644 - Deletion of expense records is not permitted...

-- 10e: Attempt to update an expense (should ALWAYS fail)
-- Expected: ERROR from trg_expenses_prevent_update
-- UPDATE expenses SET amount = 1.00 WHERE expense_id = 1;
-- Expected: ERROR 1644 - Modification of expense records is not permitted...

-- 10f: Attempt to re-open a CLOSED campaign (campaign_id 4)
-- Expected: ERROR from trg_campaigns_before_update
-- UPDATE campaigns SET status = 'ACTIVE' WHERE campaign_id = 4;
-- Expected: ERROR 1644 - Campaign status cannot be changed: CLOSED and COMPLETED are terminal states.

-- Verify triggers are registered
SELECT trigger_name, event_manipulation, event_object_table, action_timing
FROM information_schema.triggers
WHERE trigger_schema = 'ngo_donation_db'
ORDER BY event_object_table, trigger_name;
-- Expected: 7 triggers listed


-- =============================================================================
-- TEST 11: AGGREGATE QUERIES (GROUP BY / HAVING)
-- =============================================================================
SELECT '=== TEST 11: AGGREGATE QUERIES ===' AS test_section;

-- 11a: Campaigns that have collected more than 50,000 INR (HAVING clause)
SELECT
    c.campaign_id,
    c.name,
    COALESCE(SUM(CASE WHEN d.status = 'COMPLETED' THEN d.amount END), 0.00)
        AS total_collected
FROM campaigns c
LEFT JOIN donations d ON c.campaign_id = d.campaign_id
GROUP BY c.campaign_id, c.name
HAVING total_collected > 50000.00
ORDER BY total_collected DESC;
-- Expected: Campaign 1 (105000) listed; Campaign 2 (28500) excluded

-- 11b: Total donations per payment method (GROUP BY)
SELECT
    payment_method,
    COUNT(*)        AS donation_count,
    SUM(amount)     AS gross_total,
    SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END)
                    AS completed_total
FROM donations
GROUP BY payment_method
ORDER BY completed_total DESC;

-- 11c: Monthly donation trend (GROUP BY year-month)
SELECT
    DATE_FORMAT(donated_at, '%Y-%m') AS month,
    COUNT(*)                          AS total_donations,
    SUM(amount)                       AS gross_amount,
    SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END)
                                      AS completed_amount
FROM donations
GROUP BY DATE_FORMAT(donated_at, '%Y-%m')
ORDER BY month;

-- 11d: Expense breakdown by category for campaign 1
SELECT
    category,
    COUNT(*)   AS expense_count,
    SUM(amount) AS category_total
FROM expenses
WHERE campaign_id = 1
GROUP BY category
ORDER BY category_total DESC;

-- 11e: Top donors by total completed giving
SELECT
    dr.name,
    dr.email,
    SUM(CASE WHEN don.status = 'COMPLETED' THEN don.amount ELSE 0 END)
                AS total_given
FROM donors dr
JOIN donations don ON dr.donor_id = don.donor_id
GROUP BY dr.donor_id, dr.name, dr.email
ORDER BY total_given DESC
LIMIT 5;


-- =============================================================================
-- TEST 12: SUBQUERIES
-- =============================================================================
SELECT '=== TEST 12: SUBQUERY EXAMPLES ===' AS test_section;

-- 12a: Correlated subquery — campaigns where available funds > 0
SELECT
    c.campaign_id,
    c.name,
    (SELECT COALESCE(SUM(d.amount), 0.00)
     FROM donations d
     WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED')
     -
    (SELECT COALESCE(SUM(e.amount), 0.00)
     FROM expenses e WHERE e.campaign_id = c.campaign_id)
     AS available_funds
FROM campaigns c
HAVING available_funds > 0
ORDER BY available_funds DESC;

-- 12b: Donors who have donated to more than one campaign
SELECT dr.donor_id, dr.name, dr.email,
       COUNT(DISTINCT don.campaign_id) AS campaigns_donated_to
FROM donors dr
JOIN donations don ON dr.donor_id = don.donor_id
WHERE don.status = 'COMPLETED'
GROUP BY dr.donor_id, dr.name, dr.email
HAVING COUNT(DISTINCT don.campaign_id) > 1;

-- 12c: Campaigns whose total_collected exceeds 50% of their goal
SELECT
    c.campaign_id,
    c.name,
    c.goal_amount,
    COALESCE(
        (SELECT SUM(d.amount) FROM donations d
         WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'),
        0.00
    ) AS total_collected
FROM campaigns c
WHERE COALESCE(
    (SELECT SUM(d.amount) FROM donations d
     WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'),
    0.00
) >= c.goal_amount * 0.50;


-- =============================================================================
-- TEST 13: VIEW QUERIES
-- =============================================================================
SELECT '=== TEST 13: VIEWS ===' AS test_section;

-- 13a: Full campaign financial summary from view
SELECT
    campaign_id, campaign_name, status, goal_amount,
    total_collected, total_spent, available_funds, progress_percent,
    unique_donors_count, completed_donations_count
FROM vw_campaign_financials
ORDER BY campaign_id;

-- 13b: Admin dashboard view
SELECT * FROM vw_admin_dashboard;

-- 13c: Donor giving summary
SELECT donor_id, name, total_donated, completed_donations_count, campaigns_supported_count
FROM vw_donor_summary
ORDER BY total_donated DESC;

-- 13d: Enriched donation details
SELECT donation_id, donor_name, campaign_name, amount, payment_method, status, donated_at
FROM vw_donation_details
ORDER BY donated_at DESC;

-- 13e: Enriched expense details
SELECT expense_id, campaign_name, admin_name, description, amount, category, expense_date
FROM vw_expense_details
ORDER BY expense_date;


-- =============================================================================
-- TEST 14: TRANSACTION EXAMPLE (illustrative — matches BR-16 implementation)
-- Shows the full pessimistic-lock pattern used by sp_record_expense
-- =============================================================================
SELECT '=== TEST 14: TRANSACTION WITH PESSIMISTIC LOCK PATTERN ===' AS test_section;

-- This block illustrates the pattern (not a DML change — read only):
START TRANSACTION;

-- Step 1: Lock the campaign row to prevent concurrent overspend
SELECT campaign_id, name, status
FROM campaigns
WHERE campaign_id = 1
FOR UPDATE;

-- Step 2: Compute available funds while holding the lock
SELECT
    COALESCE(SUM(CASE WHEN d.status = 'COMPLETED' THEN d.amount END), 0.00) AS total_collected,
    COALESCE((SELECT SUM(e.amount) FROM expenses e WHERE e.campaign_id = 1),  0.00) AS total_spent,
    COALESCE(SUM(CASE WHEN d.status = 'COMPLETED' THEN d.amount END), 0.00) -
    COALESCE((SELECT SUM(e.amount) FROM expenses e WHERE e.campaign_id = 1),  0.00) AS available_funds
FROM donations d
WHERE d.campaign_id = 1;

-- Step 3: (In real usage) INSERT expense if amount <= available_funds, else ROLLBACK
-- For this read-only illustration we simply commit without inserting.
COMMIT;

-- =============================================================================
-- PHASE 2 TEST SUMMARY
-- =============================================================================
SELECT '=== PHASE 2 TEST SUMMARY ===' AS section;
SELECT
    (SELECT COUNT(*) FROM admins)    AS admins_count,
    (SELECT COUNT(*) FROM donors)    AS donors_count,
    (SELECT COUNT(*) FROM campaigns) AS campaigns_count,
    (SELECT COUNT(*) FROM donations) AS donations_count,
    (SELECT COUNT(*) FROM expenses)  AS expenses_count,
    (SELECT COUNT(*) FROM information_schema.views    WHERE table_schema = 'ngo_donation_db') AS views_count,
    (SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = 'ngo_donation_db' AND routine_type = 'PROCEDURE') AS procedures_count,
    (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = 'ngo_donation_db') AS triggers_count;
-- Expected: 2 admins, 5 donors, 4 campaigns, 12+ donations, 6+ expenses,
--           5 views, 4 procedures, 7 triggers
