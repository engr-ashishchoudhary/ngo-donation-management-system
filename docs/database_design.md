# Database Design — NGO Donation Management System

---

## 1. Overview

The database consists of **five entities**:

| Entity | Table Name | Description |
|--------|-----------|-------------|
| Admin | `admins` | NGO staff who manage campaigns and expenses. |
| Donor | `donors` | Registered individuals who make donations. |
| Campaign | `campaigns` | Fundraising campaigns created by admins. |
| Donation | `donations` | Individual monetary contributions from donors. |
| Expense | `expenses` | Expenditures recorded by admins against campaigns. |

---

## 2. Entity Definitions

### 2.1 Admin (`admins`)

Stores all NGO admin accounts.

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| `admin_id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique admin identifier. |
| `name` | VARCHAR(100) | NOT NULL | Full name. |
| `email` | VARCHAR(150) | NOT NULL, UNIQUE | Login email. |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt-hashed password. |
| `role` | ENUM('SUPER_ADMIN', 'ADMIN') | NOT NULL, DEFAULT 'ADMIN' | Admin role level. |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft-disable an admin account. |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Account creation timestamp. |

**Primary Key:** `admin_id`
**Unique Constraint:** `email`

---

### 2.2 Donor (`donors`)

Stores all registered donor accounts.

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| `donor_id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique donor identifier. |
| `name` | VARCHAR(100) | NOT NULL | Full name. |
| `email` | VARCHAR(150) | NOT NULL, UNIQUE | Login email. |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt-hashed password. |
| `phone` | VARCHAR(15) | NULL | Optional contact number. |
| `address` | TEXT | NULL | Optional mailing address. |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft-disable an account. |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Registration timestamp. |
| `updated_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | Last profile update timestamp. |

**Primary Key:** `donor_id`
**Unique Constraint:** `email`

---

### 2.3 Campaign (`campaigns`)

A fundraising campaign created and owned by an admin.

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| `campaign_id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique campaign identifier. |
| `admin_id` | INT | NOT NULL, FOREIGN KEY → admins(admin_id) | Admin who created it. |
| `name` | VARCHAR(200) | NOT NULL, UNIQUE | Campaign title. |
| `description` | TEXT | NULL | Detailed description. |
| `goal_amount` | DECIMAL(12,2) | NOT NULL, CHECK > 0 | Fundraising target. |
| `status` | ENUM('ACTIVE','CLOSED','COMPLETED') | NOT NULL, DEFAULT 'ACTIVE' | Lifecycle status. |
| `start_date` | DATE | NOT NULL | Campaign start date. |
| `end_date` | DATE | NOT NULL | Campaign end date. |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation timestamp. |
| `updated_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | Last modified timestamp. |

**Primary Key:** `campaign_id`
**Foreign Key:** `admin_id` → `admins(admin_id)` ON DELETE RESTRICT
**Unique Constraint:** `name`
**Check Constraint:** `end_date > start_date`, `goal_amount > 0`

> **Computed values (not stored columns):**
> - `total_collected` = `COALESCE(SUM(amount), 0.00)` from `donations` WHERE `campaign_id` = this AND `status = 'COMPLETED'`
> - `total_spent` = `COALESCE(SUM(amount), 0.00)` from `expenses` WHERE `campaign_id` = this
> - `available_funds` = `total_collected − total_spent`
>
> **Important:** `SUM()` returns `NULL` when there are no matching rows. `COALESCE(..., 0.00)` must
> always wrap the aggregate to ensure a numeric result when no donations or expenses exist yet.

---

### 2.4 Donation (`donations`)

A single monetary contribution from a donor to a campaign.

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| `donation_id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique donation identifier. |
| `donor_id` | INT | NOT NULL, FOREIGN KEY → donors(donor_id) | Donor who gave. |
| `campaign_id` | INT | NOT NULL, FOREIGN KEY → campaigns(campaign_id) | Campaign that received it. |
| `amount` | DECIMAL(12,2) | NOT NULL, CHECK > 0 | Donation amount. |
| `payment_method` | ENUM('CASH','BANK_TRANSFER','UPI','CARD','CHEQUE') | NOT NULL | How the payment was made. |
| `status` | ENUM('PENDING','COMPLETED','FAILED','REFUNDED') | NOT NULL, DEFAULT 'PENDING' | Payment status. |
| `transaction_ref` | VARCHAR(100) | NULL, UNIQUE | External payment gateway reference. |
| `notes` | TEXT | NULL | Optional remarks. |
| `donated_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Timestamp of the donation. |

**Primary Key:** `donation_id`
**Foreign Keys:**
- `donor_id` → `donors(donor_id)` ON DELETE RESTRICT
- `campaign_id` → `campaigns(campaign_id)` ON DELETE RESTRICT

**Check Constraint:** `amount > 0`

> **Business rule:** Only rows where `status = 'COMPLETED'` are included in campaign fund totals.
> Records are **never hard-deleted** to preserve financial history.

---

### 2.5 Expense (`expenses`)

An expenditure recorded by an admin and charged against a campaign.

| Column | Data Type | Constraints | Description |
|--------|-----------|-------------|-------------|
| `expense_id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique expense identifier. |
| `campaign_id` | INT | NOT NULL, FOREIGN KEY → campaigns(campaign_id) | Campaign charged. |
| `admin_id` | INT | NOT NULL, FOREIGN KEY → admins(admin_id) | Admin who recorded it. |
| `description` | VARCHAR(255) | NOT NULL | What the money was spent on. |
| `amount` | DECIMAL(12,2) | NOT NULL, CHECK > 0 | Expense amount. |
| `category` | ENUM('OPERATIONS','LOGISTICS','MARKETING','SALARIES','SUPPLIES','OTHER') | NOT NULL | Expense category. |
| `expense_date` | DATE | NOT NULL | Date the expense was incurred. |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation timestamp. |

**Primary Key:** `expense_id`
**Foreign Keys:**
- `campaign_id` → `campaigns(campaign_id)` ON DELETE RESTRICT
- `admin_id` → `admins(admin_id)` ON DELETE RESTRICT

**Check Constraint:** `amount > 0`

> **Business rule:** Before inserting, the application (Service layer) must verify:
> `new_expense_amount ≤ available_funds`.
>
> **Concurrency safety:** The check and insert must run inside a single database transaction
> using a **pessimistic lock** (`SELECT ... FOR UPDATE` on the campaign row or the aggregate
> query). Without a lock, two concurrent admin requests could both pass the funds check
> simultaneously and together overspend the campaign budget.
>
> Records are **never hard-deleted** to preserve audit trail.

---

## 3. Relationships and Cardinality

| Relationship | Type | Description |
|-------------|------|-------------|
| Admin → Campaign | One-to-Many (1:N) | One admin can create many campaigns; each campaign has exactly one creating admin. |
| Admin → Expense | One-to-Many (1:N) | One admin can record many expenses; each expense is recorded by exactly one admin. |
| Donor → Donation | One-to-Many (1:N) | One donor can make many donations; each donation belongs to exactly one donor. |
| Campaign → Donation | One-to-Many (1:N) | One campaign can receive many donations; each donation targets exactly one campaign. |
| Campaign → Expense | One-to-Many (1:N) | One campaign can have many expenses; each expense belongs to exactly one campaign. |

---

## 4. Database Constraints Summary

| Constraint Type | Applied On | Detail |
|----------------|-----------|--------|
| PRIMARY KEY | All tables | Auto-increment integer surrogate key. |
| UNIQUE | `admins.email`, `donors.email`, `campaigns.name`, `donations.transaction_ref` | Prevents duplicates. |
| FOREIGN KEY | `campaigns.admin_id`, `donations.donor_id`, `donations.campaign_id`, `expenses.campaign_id`, `expenses.admin_id` | Referential integrity; all set to ON DELETE RESTRICT. |
| NOT NULL | All required columns | Ensures essential data is always present. |
| CHECK | `goal_amount > 0`, `donation.amount > 0`, `expense.amount > 0`, `end_date > start_date` | Enforces valid data ranges. |
| DEFAULT | `status`, `is_active`, `created_at`, `updated_at` | Sensible defaults for new records. |
| ENUM | `status` fields, `payment_method`, `category`, `role` | Restricts values to a controlled vocabulary. |

---

## 5. Normalization

The schema is normalised to **Third Normal Form (3NF)**.

### First Normal Form (1NF) ✅
- Every table has a primary key.
- All column values are atomic (no repeating groups or arrays).
- Each column holds a single type of value.

### Second Normal Form (2NF) ✅
- All tables use a single-column surrogate primary key (no composite PKs).
- Therefore, there can be no partial dependency; every non-key column depends on the whole key.

### Third Normal Form (3NF) ✅
- No transitive dependencies exist.
- Computed values (`total_collected`, `total_spent`, `available_funds`) are **not stored** as columns; they are derived at query time using SQL aggregates. This prevents update anomalies.
- `campaign.admin_id` stores only the FK reference, not any admin attribute (name, email, etc.).
- `donation.donor_id` and `donation.campaign_id` store only FK references.
- `expense.campaign_id` and `expense.admin_id` store only FK references.

---

## 6. Design Decisions

| Decision | Reason |
|----------|--------|
| `DECIMAL(12,2)` for monetary values | Avoids floating-point rounding errors. Supports amounts up to ₹9,999,999,999.99. |
| Computed fund totals (not stored) | Prevents inconsistency between stored and real values; always accurate. |
| `ON DELETE RESTRICT` on all FKs | Prevents accidental deletion of a donor/admin/campaign that still has linked records. |
| `ENUM` for status and category fields | Restricts values at the database level; easier to validate without additional lookup tables. |
| `is_active` on Admin and Donor | Allows soft-disabling accounts without deleting financial history. |
| No hard-delete on Donation/Expense | Preserves full financial audit trail as required by business rules. |
| Separate `Admin` and `Donor` tables | Different attributes and roles; merging them would complicate access control. |
| `updated_at` on Admin, Donor, Campaign | Provides a last-modified timestamp for cache invalidation and audit purposes. |
| Donation status transitions enforced in Service layer | PENDING → COMPLETED / FAILED, COMPLETED → REFUNDED only. Prevents invalid reversals (e.g. COMPLETED back to PENDING would inflate collected totals). |
| Campaign status transitions are terminal | CLOSED and COMPLETED cannot revert to ACTIVE. Enforced in Service layer. |
| Pessimistic locking for expense insertion | `SELECT ... FOR UPDATE` on the campaign row inside a transaction prevents concurrent admins from simultaneously passing the available-funds check and overspending the budget. |
| `expense_date ≥ campaign.start_date` validated in Service layer | Cannot be enforced with a single-table CHECK constraint (cross-table comparison); must be validated in the application before inserting. |

---

*Document version: 1.1 | Phase: 1 — Planning (reviewed & corrected)*
