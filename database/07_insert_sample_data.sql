-- =============================================================================
-- FILE: 07_insert_sample_data.sql
-- PROJECT: NGO Donation Management System
-- PURPOSE: Insert realistic Indian sample data for development and testing
-- DEPENDS ON: 02_create_tables.sql through 06_create_triggers.sql
-- =============================================================================
-- NOTES:
--   • Passwords below are placeholder bcrypt hashes.
--     In production, Spring Boot will generate real bcrypt hashes.
--     The placeholder string represents bcrypt("Admin@123") and bcrypt("Donor@123").
--   • Monetary amounts are in Indian Rupees (INR).
--   • All foreign-key references use explicit IDs to ensure correctness.
--   • Triggers are ACTIVE during this insert, so business rules are enforced.
--     Donations are only inserted into ACTIVE campaigns.
-- =============================================================================

USE ngo_donation_db;

-- Temporarily disable safe-update mode for seeding
SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- SECTION 1: ADMINS (2 records)
-- =============================================================================
INSERT INTO admins (name, email, password_hash, role, is_active) VALUES
(
    'Priya Sharma',
    'priya.sharma@sewngo.org',
    -- placeholder for bcrypt("Admin@123")
    '$2a$10$PH.aHGFakeBcryptHashAdminOneXXXXXXXXXXXXXXXXXXXXXXX',
    'SUPER_ADMIN',
    TRUE
),
(
    'Rajan Mehta',
    'rajan.mehta@sewngo.org',
    -- placeholder for bcrypt("Admin@123")
    '$2a$10$PH.aHGFakeBcryptHashAdminTwoXXXXXXXXXXXXXXXXXXXXXXX',
    'ADMIN',
    TRUE
);
-- admin_id 1 = Priya Sharma (SUPER_ADMIN)
-- admin_id 2 = Rajan Mehta  (ADMIN)


-- =============================================================================
-- SECTION 2: DONORS (5 records)
-- =============================================================================
INSERT INTO donors (name, email, password_hash, phone, address, is_active) VALUES
(
    'Arjun Nair',
    'arjun.nair@gmail.com',
    '$2a$10$PH.aHGFakeBcryptHashDonorOneXXXXXXXXXXXXXXXXXXXXXXX',
    '9876543210',
    '14, MG Road, Bengaluru, Karnataka 560001',
    TRUE
),
(
    'Sunita Patel',
    'sunita.patel@yahoo.com',
    '$2a$10$PH.aHGFakeBcryptHashDonorTwoXXXXXXXXXXXXXXXXXXXXXXX',
    '9123456789',
    '7, Nehru Nagar, Ahmedabad, Gujarat 380015',
    TRUE
),
(
    'Vikram Reddy',
    'vikram.reddy@outlook.com',
    '$2a$10$PH.aHGFakeBcryptHashDonorThreeXXXXXXXXXXXXXXXXXXXXX',
    '8800123456',
    '22, Jubilee Hills, Hyderabad, Telangana 500033',
    TRUE
),
(
    'Meena Krishnan',
    'meena.krishnan@gmail.com',
    '$2a$10$PH.aHGFakeBcryptHashDonorFourXXXXXXXXXXXXXXXXXXXXXX',
    '9988776655',
    '5, Anna Nagar, Chennai, Tamil Nadu 600040',
    TRUE
),
(
    'Rohit Joshi',
    'rohit.joshi@protonmail.com',
    '$2a$10$PH.aHGFakeBcryptHashDonorFiveXXXXXXXXXXXXXXXXXXXXXX',
    '7700112233',
    '31, Shivaji Park, Mumbai, Maharashtra 400028',
    TRUE
);
-- donor_id 1 = Arjun Nair
-- donor_id 2 = Sunita Patel
-- donor_id 3 = Vikram Reddy
-- donor_id 4 = Meena Krishnan
-- donor_id 5 = Rohit Joshi


-- =============================================================================
-- SECTION 3: CAMPAIGNS (4 records)
-- All created by admin_id 1 (Priya) except one by admin_id 2 (Rajan).
-- start_date and end_date satisfy the CHECK constraint end_date > start_date.
-- =============================================================================
INSERT INTO campaigns (admin_id, name, description, goal_amount, status, start_date, end_date) VALUES
(
    1,
    'Swachh Jal Abhiyan',
    'Provide safe drinking water to 10 rural villages in Rajasthan by installing solar-powered purification units.',
    500000.00,
    'ACTIVE',
    '2026-10-01',
    '2027-03-31'
),
(
    1,
    'Shiksha Roshni',
    'Distribute school supplies — notebooks, pens, and backpacks — to 500 underprivileged children in government schools across Uttar Pradesh.',
    200000.00,
    'ACTIVE',
    '2026-09-01',
    '2027-01-31'
),
(
    2,
    'Arogya Setu Medical Camp',
    'Conduct free health check-up camps and distribute medicines in tribal areas of Jharkhand.',
    350000.00,
    'COMPLETED',
    '2026-05-01',
    '2026-10-31'
),
(
    1,
    'Vriksharopan 2026',
    'Plant 5,000 saplings along highway NH-48 and urban parks in Delhi NCR with 1-year maintenance support.',
    150000.00,
    'CLOSED',
    '2026-04-01',
    '2026-09-30'
);
-- campaign_id 1 = Swachh Jal Abhiyan      (ACTIVE)
-- campaign_id 2 = Shiksha Roshni          (ACTIVE)
-- campaign_id 3 = Arogya Setu Med Camp    (COMPLETED)
-- campaign_id 4 = Vriksharopan 2026       (CLOSED)

-- NOTE: Triggers trg_donations_before_insert enforce that donations can only
-- go to ACTIVE campaigns (campaign_id 1 and 2 in this dataset).


-- =============================================================================
-- SECTION 4: DONATIONS (12 records — various statuses)
-- Only campaigns 1 and 2 are ACTIVE, so all donations reference them.
-- (Campaign 3 and 4 are COMPLETED/CLOSED — inserting donations would trigger
--  trg_donations_before_insert and fail, as designed.)
-- =============================================================================
INSERT INTO donations
    (donor_id, campaign_id, amount, payment_method, status, transaction_ref, notes)
VALUES
-- ---- Campaign 1: Swachh Jal Abhiyan (ACTIVE) ----
(1, 1, 25000.00, 'UPI',           'COMPLETED', 'UPI-SJA-001-ARJUN',   'Happy to support clean water.'),
(2, 1, 15000.00, 'BANK_TRANSFER', 'COMPLETED', 'NEFT-SJA-002-SUNITA', 'Monthly contribution.'),
(3, 1, 50000.00, 'CARD',          'COMPLETED', 'CARD-SJA-003-VIKRAM', 'Corporate matching gift.'),
(4, 1,  5000.00, 'UPI',           'COMPLETED', 'UPI-SJA-004-MEENA',   NULL),
(5, 1, 10000.00, 'CHEQUE',        'COMPLETED', 'CHQ-SJA-005-ROHIT',   'Cheque no. 004521.'),
(1, 1,  8000.00, 'UPI',           'PENDING',    NULL,                  'Second donation — awaiting confirmation.'),
(2, 1,  3000.00, 'CASH',          'FAILED',     NULL,                  'Cash payment not accepted at kiosk.'),
(3, 1, 20000.00, 'BANK_TRANSFER', 'REFUNDED',  'NEFT-SJA-008-VIKRAM', 'Duplicate transfer — refunded.'),

-- ---- Campaign 2: Shiksha Roshni (ACTIVE) ----
(4, 2, 12000.00, 'UPI',           'COMPLETED', 'UPI-SR-009-MEENA',  'Books for children.'),
(5, 2,  7500.00, 'CARD',          'COMPLETED', 'CARD-SR-010-ROHIT', 'Annual school fund.'),
(1, 2,  4000.00, 'UPI',           'PENDING',    NULL,                'Processing.'),
(2, 2,  9000.00, 'BANK_TRANSFER', 'COMPLETED', 'NEFT-SR-012-SUNITA','Quarterly donation.');

-- Summary of COMPLETED donations (what counts toward campaign funds):
--   Campaign 1: 25000 + 15000 + 50000 + 5000 + 10000 = 105000.00
--   Campaign 2: 12000 + 7500 + 9000                  =  28500.00


-- =============================================================================
-- SECTION 5: EXPENSES (6 records)
-- Must reference campaigns with sufficient available funds.
-- Campaign 1 available = 105000.00
-- Campaign 2 available = 28500.00
-- =============================================================================
INSERT INTO expenses (campaign_id, admin_id, description, amount, category, expense_date) VALUES
-- ---- Campaign 1: Swachh Jal Abhiyan ----
(1, 1, 'Solar water purification unit — Village Ramgarh',    35000.00, 'SUPPLIES',    '2026-10-15'),
(1, 2, 'Transportation of purifier units from Jaipur depot', 12000.00, 'LOGISTICS',   '2026-10-20'),
(1, 1, 'Community awareness campaign materials',              5000.00, 'MARKETING',   '2026-11-01'),
(1, 2, 'Site visit allowances for field staff (Oct)',         8000.00, 'OPERATIONS',  '2026-11-05'),

-- ---- Campaign 2: Shiksha Roshni ----
(2, 1, 'Notebooks and stationery — Lucknow batch 1',        15000.00, 'SUPPLIES',    '2026-09-20'),
(2, 2, 'Courier charges for school supply packages',          4000.00, 'LOGISTICS',   '2026-09-25');

-- Verification after inserts:
--   Campaign 1: total_spent = 35000 + 12000 + 5000 + 8000 = 60000.00
--               available   = 105000 - 60000              = 45000.00
--   Campaign 2: total_spent = 15000 + 4000               = 19000.00
--               available   = 28500  - 19000              = 9500.00

-- Re-enable safe-update mode
SET SQL_SAFE_UPDATES = 1;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================

-- Show all admins
SELECT admin_id, name, email, role, is_active FROM admins;

-- Show all donors
SELECT donor_id, name, email, phone, is_active FROM donors;

-- Show campaign financial summary (uses computed aggregates)
SELECT
    campaign_id,
    name,
    status,
    goal_amount,
    COALESCE((SELECT SUM(amount) FROM donations d
              WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'), 0.00)
              AS total_collected,
    COALESCE((SELECT SUM(amount) FROM expenses e
              WHERE e.campaign_id = c.campaign_id), 0.00)
              AS total_spent,
    COALESCE((SELECT SUM(amount) FROM donations d
              WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'), 0.00) -
    COALESCE((SELECT SUM(amount) FROM expenses e
              WHERE e.campaign_id = c.campaign_id), 0.00)
              AS available_funds
FROM campaigns c
ORDER BY campaign_id;

-- Show all donations with status breakdown
SELECT
    don.donation_id,
    dr.name  AS donor_name,
    c.name   AS campaign_name,
    don.amount,
    don.payment_method,
    don.status,
    don.donated_at
FROM donations don
JOIN donors    dr ON don.donor_id    = dr.donor_id
JOIN campaigns c  ON don.campaign_id = c.campaign_id
ORDER BY don.donation_id;

-- Show all expenses
SELECT
    ex.expense_id,
    c.name  AS campaign_name,
    a.name  AS admin_name,
    ex.description,
    ex.amount,
    ex.category,
    ex.expense_date
FROM expenses ex
JOIN campaigns c ON ex.campaign_id = c.campaign_id
JOIN admins    a ON ex.admin_id    = a.admin_id
ORDER BY ex.expense_id;
