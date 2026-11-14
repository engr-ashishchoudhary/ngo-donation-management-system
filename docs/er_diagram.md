# ER Diagram — NGO Donation Management System

---

## Entity-Relationship Diagram (Mermaid)

```mermaid
erDiagram

    ADMINS {
        int     admin_id        PK  "AUTO_INCREMENT"
        varchar name            "NOT NULL"
        varchar email           "NOT NULL, UNIQUE"
        varchar password_hash   "NOT NULL"
        enum    role            "SUPER_ADMIN | ADMIN"
        boolean is_active       "DEFAULT TRUE"
        datetime created_at     "DEFAULT NOW()"
    }

    DONORS {
        int     donor_id        PK  "AUTO_INCREMENT"
        varchar name            "NOT NULL"
        varchar email           "NOT NULL, UNIQUE"
        varchar password_hash   "NOT NULL"
        varchar phone           "NULL"
        text    address         "NULL"
        boolean is_active       "DEFAULT TRUE"
        datetime created_at     "DEFAULT NOW()"
        datetime updated_at     "ON UPDATE CURRENT_TIMESTAMP"
    }

    CAMPAIGNS {
        int     campaign_id     PK  "AUTO_INCREMENT"
        int     admin_id        FK  "→ admins(admin_id)"
        varchar name            "NOT NULL, UNIQUE"
        text    description     "NULL"
        decimal goal_amount     "DECIMAL(12,2), > 0"
        enum    status          "ACTIVE | CLOSED | COMPLETED"
        date    start_date      "NOT NULL"
        date    end_date        "NOT NULL, > start_date"
        datetime created_at     "DEFAULT NOW()"
        datetime updated_at     "ON UPDATE NOW()"
    }

    DONATIONS {
        int     donation_id     PK  "AUTO_INCREMENT"
        int     donor_id        FK  "→ donors(donor_id)"
        int     campaign_id     FK  "→ campaigns(campaign_id)"
        decimal amount          "DECIMAL(12,2), > 0"
        enum    payment_method  "CASH | BANK_TRANSFER | UPI | CARD | CHEQUE"
        enum    status          "PENDING | COMPLETED | FAILED | REFUNDED"
        varchar transaction_ref "NULL, UNIQUE"
        text    notes           "NULL"
        datetime donated_at     "DEFAULT NOW()"
    }

    EXPENSES {
        int     expense_id      PK  "AUTO_INCREMENT"
        int     campaign_id     FK  "→ campaigns(campaign_id)"
        int     admin_id        FK  "→ admins(admin_id)"
        varchar description     "NOT NULL"
        decimal amount          "DECIMAL(12,2), > 0"
        enum    category        "OPERATIONS | LOGISTICS | MARKETING | SALARIES | SUPPLIES | OTHER"
        date    expense_date    "NOT NULL"
        datetime created_at     "DEFAULT NOW()"
    }

    ADMINS    ||--o{ CAMPAIGNS : "creates"
    ADMINS    ||--o{ EXPENSES  : "records"
    DONORS    ||--o{ DONATIONS : "makes"
    CAMPAIGNS ||--o{ DONATIONS : "receives"
    CAMPAIGNS ||--o{ EXPENSES  : "has"
```

---

## Relationship Descriptions

| Relationship | Notation | Meaning |
|-------------|----------|---------|
| `ADMINS` → `CAMPAIGNS` | `\|\|--o{` | One admin creates zero or more campaigns. Each campaign belongs to exactly one admin. |
| `ADMINS` → `EXPENSES` | `\|\|--o{` | One admin records zero or more expenses. Each expense is recorded by exactly one admin. |
| `DONORS` → `DONATIONS` | `\|\|--o{` | One donor makes zero or more donations. Each donation belongs to exactly one donor. |
| `CAMPAIGNS` → `DONATIONS` | `\|\|--o{` | One campaign receives zero or more donations. Each donation targets exactly one campaign. |
| `CAMPAIGNS` → `EXPENSES` | `\|\|--o{` | One campaign has zero or more expenses. Each expense belongs to exactly one campaign. |

---

## Key Legend

| Symbol | Meaning |
|--------|---------|
| `PK` | Primary Key |
| `FK` | Foreign Key |
| `\|\|` | Exactly one (mandatory) |
| `o{` | Zero or more (optional many) |

---

## Computed Values (Not in Diagram — Derived at Query Time)

These are **not stored columns**; they are calculated dynamically using SQL:

| Value | Formula |
|-------|---------|
| `total_collected` | `COALESCE(SUM(amount), 0.00) FROM donations WHERE campaign_id = ? AND status = 'COMPLETED'` |
| `total_spent` | `COALESCE(SUM(amount), 0.00) FROM expenses WHERE campaign_id = ?` |
| `available_funds` | `total_collected − total_spent` |

> **Note:** `COALESCE(..., 0.00)` is mandatory. `SUM()` returns `NULL` when no rows match;
> without `COALESCE`, the `available_funds` formula produces `NULL` instead of `0.00`
> for a new campaign with no donations or expenses yet.

---

*Document version: 1.1 | Phase: 1 — Planning (reviewed & corrected)*
