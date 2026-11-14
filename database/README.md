# Database — NGO Donation Management System

> **Phase:** 2 — MySQL Implementation  
> **Database:** `ngo_donation_db` (MySQL 8.0)  
> **Status:** Scripts ready. Not yet executed — MySQL credentials not assumed.

---

## Folder Contents

```
database/
├── 01_create_database.sql     — Create the ngo_donation_db database
├── 02_create_tables.sql       — All five tables with constraints
├── 03_create_indexes.sql      — Performance indexes
├── 04_create_views.sql        — 5 reporting views
├── 05_create_procedures.sql   — 4 stored procedures
├── 06_create_triggers.sql     — 7 business-rule triggers
├── 07_insert_sample_data.sql  — 2 admins, 5 donors, 4 campaigns, 12 donations, 6 expenses
├── 08_test_queries.sql        — 14 test sections covering all requirements
└── README.md                  — This file
```

---

## How to Run the Scripts

### Prerequisites

| Requirement | Detail |
|------------|--------|
| MySQL Server | Version **8.0.16 or higher** (CHECK constraints are fully enforced from 8.0.16) |
| MySQL Client | MySQL Shell (`mysqlsh`) or MySQL Workbench or `mysql` CLI |
| Privileges | `CREATE DATABASE`, `CREATE TABLE`, `CREATE VIEW`, `CREATE ROUTINE`, `CREATE TRIGGER`, `INSERT`, `SELECT` |

### Execution Order — Run These Files in Sequence

**Always run in this exact order.** Each file depends on the previous ones.

```bash
# Replace <user> and <host> with your MySQL credentials
mysql -u <user> -p -h <host> < 01_create_database.sql
mysql -u <user> -p -h <host> < 02_create_tables.sql
mysql -u <user> -p -h <host> < 03_create_indexes.sql
mysql -u <user> -p -h <host> < 04_create_views.sql
mysql -u <user> -p -h <host> < 05_create_procedures.sql
mysql -u <user> -p -h <host> < 06_create_triggers.sql
mysql -u <user> -p -h <host> < 07_insert_sample_data.sql
mysql -u <user> -p -h <host> < 08_test_queries.sql
```

### Alternative — Run from MySQL Workbench or Shell

Open each file in MySQL Workbench and click **Execute (⚡)** in order, or in MySQL Shell:

```sql
SOURCE /full/path/to/database/01_create_database.sql;
SOURCE /full/path/to/database/02_create_tables.sql;
-- ... repeat for each file in order
```

> **Important:** `01_create_database.sql` contains `DROP DATABASE IF EXISTS ngo_donation_db`.  
> Do **not** run it on a database with existing production data.

---

## Database Structure

### Five Tables

| Table | Rows (sample) | Purpose |
|-------|-------------|---------|
| `admins` | 2 | NGO staff accounts |
| `donors` | 5 | Registered donor accounts |
| `campaigns` | 4 | Fundraising campaigns |
| `donations` | 12 | Individual donor contributions |
| `expenses` | 6 | Campaign expenditures |

### Table Relationships (all One-to-Many)

```
admins ──┬──────────────── campaigns ──┬──── donations ──── donors
         │                             │
         └──── expenses ───────────────┘
```

| Parent Table | Child Table | Foreign Key |
|-------------|------------|-------------|
| `admins` | `campaigns` | `campaigns.admin_id` |
| `admins` | `expenses` | `expenses.admin_id` |
| `donors` | `donations` | `donations.donor_id` |
| `campaigns` | `donations` | `donations.campaign_id` |
| `campaigns` | `expenses` | `expenses.campaign_id` |

All foreign keys use `ON DELETE RESTRICT ON UPDATE CASCADE`.

---

## Five Views

| View | Purpose |
|------|---------|
| `vw_campaign_financials` | Full financial summary per campaign: collected, spent, available, progress % |
| `vw_donation_details` | Donations joined with donor and campaign names |
| `vw_expense_details` | Expenses joined with campaign and admin names |
| `vw_admin_dashboard` | Single-row NGO-wide metrics for the admin home page |
| `vw_donor_summary` | Per-donor giving totals and campaign counts |

### Key Financial Query Pattern

All monetary totals use `COALESCE(SUM(), 0.00)` to return `0.00` instead of `NULL` for campaigns with no data yet:

```sql
-- Total collected (only COMPLETED donations count — BR-05)
COALESCE(
    (SELECT SUM(d.amount) FROM donations d
     WHERE d.campaign_id = c.campaign_id AND d.status = 'COMPLETED'),
    0.00
) AS total_collected
```

---

## Four Stored Procedures

| Procedure | Purpose |
|-----------|---------|
| `sp_record_expense` | Safely inserts an expense inside a transaction with `SELECT ... FOR UPDATE` pessimistic lock (BR-16) |
| `sp_update_donation_status` | Enforces donation status state machine: PENDING→COMPLETED, PENDING→FAILED, COMPLETED→REFUNDED only (BR-13) |
| `sp_update_campaign_status` | Enforces terminal campaign status: ACTIVE→CLOSED or COMPLETED; terminal states cannot change (BR-14) |
| `sp_get_campaign_financials` | Returns real-time financial summary for one campaign |

### How to Call a Procedure

```sql
-- Record an expense safely (with pessimistic lock):
CALL sp_record_expense(
    1,            -- campaign_id
    1,            -- admin_id
    'Water pump parts',  -- description
    15000.00,     -- amount (INR)
    'SUPPLIES',   -- category
    '2026-11-20', -- expense_date
    @expense_id,  -- OUT: new expense_id (0 on failure)
    @message      -- OUT: success/error message
);
SELECT @expense_id, @message;
```

---

## Seven Triggers

| Trigger | Table | Event | Purpose |
|---------|-------|-------|---------|
| `trg_donations_before_insert` | donations | BEFORE INSERT | Reject donations to non-ACTIVE campaigns (BR-04) |
| `trg_donations_before_update` | donations | BEFORE UPDATE | Enforce status state machine (BR-13) |
| `trg_donations_prevent_delete` | donations | BEFORE DELETE | Block all hard-deletes (BR-10) |
| `trg_expenses_before_insert` | expenses | BEFORE INSERT | Last-resort overspend check (BR-06) |
| `trg_expenses_prevent_delete` | expenses | BEFORE DELETE | Block all hard-deletes (BR-11) |
| `trg_expenses_prevent_update` | expenses | BEFORE UPDATE | Expenses are immutable (BR-11) |
| `trg_campaigns_before_update` | campaigns | BEFORE UPDATE | Enforce terminal status states (BR-14) |

---

## Important SQL Concepts Used

| Concept | Used In | Why |
|---------|---------|-----|
| `DECIMAL(12,2)` | All monetary columns | Exact arithmetic — no floating-point errors |
| `COALESCE(SUM(), 0.00)` | Views, procedures, queries | `SUM()` returns NULL for empty sets; COALESCE ensures 0.00 |
| `SELECT ... FOR UPDATE` | `sp_record_expense` | Pessimistic lock prevents concurrent overspend (BR-16) |
| `START TRANSACTION / COMMIT / ROLLBACK` | `sp_record_expense` | Atomic check-then-insert |
| `SIGNAL SQLSTATE '45000'` | All triggers | Raise custom application error from trigger |
| `CHECK` constraints | `campaigns`, `donations`, `expenses` | Enforce positive amounts and valid date ranges at DB level |
| `ENUM` | Status, role, category, payment method columns | Controlled vocabulary enforced at DB level |
| `ON DELETE RESTRICT` | All foreign keys | Prevents orphaned financial records |
| Correlated subqueries | Views, test queries | Compute per-campaign totals inline |
| `LEFT JOIN` | Donor summary view | Include donors with zero donations |
| `GROUP BY / HAVING` | Test queries 11a–11e | Aggregate filtering (e.g. campaigns > 50,000 collected) |
| Window functions (`OVER`) | Test query 11 | Percentage of campaign total per status |
| `DATE_FORMAT` | Test query 11c | Monthly donation trend grouping |
| `INFORMATION_SCHEMA` | Verification queries | Confirm tables, indexes, views, procedures, triggers exist |

---

## Sample Data Summary

After running `07_insert_sample_data.sql`:

| Campaign | Status | Goal | Collected | Spent | Available |
|---------|--------|------|-----------|-------|-----------|
| Swachh Jal Abhiyan | ACTIVE | ₹5,00,000 | ₹1,05,000 | ₹60,000 | ₹45,000 |
| Shiksha Roshni | ACTIVE | ₹2,00,000 | ₹28,500 | ₹19,000 | ₹9,500 |
| Arogya Setu Medical Camp | COMPLETED | ₹3,50,000 | ₹0 | ₹0 | ₹0 |
| Vriksharopan 2026 | CLOSED | ₹1,50,000 | ₹0 | ₹0 | ₹0 |

> Campaigns 3 and 4 have no donations in the sample data because their status is COMPLETED/CLOSED, and the trigger `trg_donations_before_insert` correctly blocks donations to non-ACTIVE campaigns.

---

## Unresolved Items

| Item | Detail |
|------|--------|
| Actual password hashes | Sample data contains placeholder bcrypt strings. Spring Boot will generate real hashes in Phase 3. |
| MySQL credentials | Not assumed. Supply your own host/user/password when running the scripts. |
| Production `DROP DATABASE` guard | Remove line 7 of `01_create_database.sql` before running in a production environment. |

---

*Phase 2 — MySQL Implementation | Ready for Phase 3: Spring Boot Backend*
