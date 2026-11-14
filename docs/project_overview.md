# Project Overview — NGO Donation Management System

---

## 1. Project Introduction

The **NGO Donation Management System** is a full-stack web application designed to help
non-governmental organisations (NGOs) manage fundraising campaigns, track donor
contributions, and monitor expenditures — all from a single, secure platform.

The system provides a transparent, audit-friendly record of every donation and every
rupee spent, making it easier for NGOs to maintain donor trust and regulatory compliance.

---

## 2. Problem Statement

Many small and mid-sized NGOs still rely on spreadsheets, paper registers, or disconnected
tools to manage donations and expenses. This leads to:

- **Lack of transparency** — donors cannot see how their money is used.
- **Manual errors** — amounts miscalculated, entries duplicated or lost.
- **No real-time visibility** — campaign progress is unknown until someone compiles a report.
- **Difficulty in audits** — financial history is scattered across files and notebooks.
- **No access control** — anyone with file access can modify records.

The NGO Donation Management System solves these problems with a centralised, role-based
web application backed by a relational database.

---

## 3. Objectives

| # | Objective |
|---|-----------|
| 1 | Provide a secure, role-based platform for NGO admins and donors. |
| 2 | Allow admins to create and manage fundraising campaigns. |
| 3 | Record and track donations from individual donors. |
| 4 | Track campaign expenses and enforce budget controls. |
| 5 | Provide real-time campaign progress (funds collected vs. goal). |
| 6 | Maintain a complete, immutable financial history. |
| 7 | Expose a clean REST API for future mobile/third-party integrations. |

---

## 4. Main Features

### Admin Features
- Secure login with role-based access.
- Create, update, and close fundraising campaigns.
- View all donations across all campaigns.
- Record campaign expenses (approved by admin).
- Enforce budget: expenses cannot exceed available funds.
- Dashboard with key metrics (total raised, total spent, active campaigns).

### Donor Features
- Register and log in securely.
- Browse active campaigns.
- Make donations (single or multiple) to any campaign.
- View personal donation history and status.
- Download or view donation receipts.

### System Features
- Real-time campaign progress (collected / goal).
- Only **completed** donations count toward collected funds.
- Pending, failed, and refunded donations are excluded from totals.
- Complete audit trail of all financial transactions.

---

## 5. Technology Stack

| Layer | Technology | Version / Notes |
|-------|-----------|-----------------|
| **Frontend** | React | v18+ |
| **Frontend Build** | Vite | v5+ |
| **Frontend Language** | JavaScript (JSX) | ES2022+ |
| **Backend** | Java + Spring Boot | Java 17, Spring Boot 3.x |
| **Build Tool** | Apache Maven | 3.9+ |
| **Database** | MySQL | 8.0 |
| **ORM** | Spring Data JPA / Hibernate | Included in Spring Boot |
| **Security** | Spring Security + JWT | Stateless authentication |
| **API Style** | REST (JSON) | — |

---

*Document version: 1.0 | Phase: 1 — Planning*
