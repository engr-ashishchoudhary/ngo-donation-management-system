# Requirements — NGO Donation Management System

---

## 1. User Roles

| Role | Description |
|------|-------------|
| **Admin** | NGO staff member. Manages campaigns, views all donations, records and approves expenses. |
| **Donor** | Registered member of the public. Browses campaigns and makes donations. |

> **Note:** For Phase 1 scope there are two roles only. The `Admin` entity stores all
> admin accounts in the database with a role field for future multi-level access control.

---

## 2. Functional Requirements

### 2.1 Authentication & Authorisation
| ID | Requirement |
|----|-------------|
| FR-01 | An Admin must be able to register and log in with an email and password. |
| FR-02 | A Donor must be able to register and log in with an email and password. |
| FR-03 | Passwords must be stored hashed (bcrypt). |
| FR-04 | Sessions must be managed via JWT tokens. |
| FR-05 | Only Admins can create or close campaigns. |
| FR-06 | Only Admins can record expenses. Expense records cannot be deleted by anyone (immutable). |
| FR-07 | Donors can view campaigns and their own donation history only. |

### 2.2 Campaign Management
| ID | Requirement |
|----|-------------|
| FR-08 | Admins can create a campaign with a name, description, goal amount, start date, and end date. |
| FR-09 | Admins can update campaign details while the campaign is active. |
| FR-10 | Admins can manually close a campaign (set status to CLOSED). |
| FR-10a | Campaign status transitions: ACTIVE → CLOSED (manual by admin), ACTIVE → COMPLETED (manual by admin when goal is reached). CLOSED and COMPLETED are terminal states; they cannot be re-opened. |
| FR-11 | A campaign has a status: ACTIVE, CLOSED, or COMPLETED. |
| FR-12 | A campaign must have a unique name. |
| FR-13 | The system must calculate the total collected amount dynamically from completed donations. |
| FR-14 | The system must calculate the total spent amount dynamically from recorded expenses. |
| FR-15 | Available funds = total collected − total spent. |

### 2.3 Donation Management
| ID | Requirement |
|----|-------------|
| FR-16 | A Donor can make a donation to any active campaign. |
| FR-17 | Each donation must record: donor, campaign, amount, payment method, status, and timestamp. |
| FR-18 | Donation status values: PENDING, COMPLETED, FAILED, REFUNDED. |
| FR-18a | Allowed status transitions: PENDING → COMPLETED, PENDING → FAILED, COMPLETED → REFUNDED. No other transitions are permitted. A COMPLETED donation cannot revert to PENDING. |
| FR-19 | Only COMPLETED donations count toward campaign collected funds. |
| FR-20 | Donation records must never be deleted (immutable financial history). |
| FR-21 | Admins can view all donations; donors can view only their own. |
| FR-22 | An Admin can update a donation status (e.g. mark a pending donation completed). |

### 2.4 Expense Management
| ID | Requirement |
|----|-------------|
| FR-23 | Admins can record an expense against a campaign. |
| FR-24 | Each expense must record: campaign, admin, description, amount, category, and date. |
| FR-25 | The system must reject any expense that would exceed the campaign's available funds. |
| FR-26 | Expense records must never be deleted (immutable audit trail). |
| FR-27 | Admins can view all expenses for any campaign. |

### 2.5 Reporting & Dashboard
| ID | Requirement |
|----|-------------|
| FR-28 | Admin dashboard shows: total donations received, total expenses, active campaigns count, donor count. |
| FR-29 | Each campaign page shows: goal, collected, spent, and available funds. |
| FR-30 | Donors can view a history of their own donations with statuses. |

---

## 3. Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Security | All passwords must be hashed with bcrypt (strength ≥ 10). |
| NFR-02 | Security | API endpoints must be protected via JWT; unauthenticated requests return 401. |
| NFR-03 | Security | Role-based access: unauthorised role returns 403. |
| NFR-04 | Data Integrity | Monetary values stored as DECIMAL(12,2) — no floating-point rounding errors. |
| NFR-05 | Data Integrity | Foreign-key constraints enforced at the database level. |
| NFR-06 | Data Integrity | No donation or expense record may be hard-deleted. |
| NFR-07 | Performance | Campaign fund totals calculated with optimised SQL aggregates, not in application memory. |
| NFR-08 | Usability | API responses must follow a consistent JSON structure with descriptive error messages. |
| NFR-09 | Maintainability | Code follows standard layered architecture: Controller → Service → Repository. |
| NFR-10 | Scalability | The database schema supports future addition of features (e.g. recurring donations, reports). |

---

## 4. Business Rules

| ID | Rule |
|----|------|
| BR-01 | A donation amount must be greater than ₹0.00. |
| BR-02 | A campaign goal amount must be greater than ₹0.00. |
| BR-03 | A campaign's end date must be after its start date. |
| BR-04 | Donations can only be made to campaigns with status = ACTIVE. |
| BR-05 | Only COMPLETED donations are included in the collected funds total. |
| BR-06 | An expense cannot be recorded if it exceeds available funds (collected − spent). |
| BR-07 | An expense amount must be greater than ₹0.00. |
| BR-08 | A donor's email address must be unique across the system. |
| BR-09 | An admin's email address must be unique across the system. |
| BR-10 | Donation records are immutable; they may only have their status updated. |
| BR-11 | Expense records are immutable; they may not be modified or deleted after creation. |
| BR-13 | Donation status transitions are strictly controlled: PENDING → COMPLETED, PENDING → FAILED, COMPLETED → REFUNDED only. |
| BR-14 | Campaign status transitions are terminal: once CLOSED or COMPLETED, the status cannot be changed back to ACTIVE. |
| BR-15 | An expense date must not be before the campaign's start date. This is enforced by the Service layer. |
| BR-16 | The Service layer must use a database-level lock (pessimistic locking) when checking available funds before inserting an expense, to prevent concurrent admins from overspending the campaign budget. |
| BR-12 | Campaign names must be unique. |

---

*Document version: 1.1 | Phase: 1 — Planning (reviewed & corrected)*
