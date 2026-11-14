# NGO Donation Management System

A web-based NGO Donation Management System developed as a DBMS academic project. The application is planned to help an NGO manage donors, fundraising campaigns, donations, and campaign expenses through a centralized system.

> **Project status:** Phase 1 (project planning and database design) is complete. Database implementation and application development are planned next.

## Objectives

- Maintain donor and administrator records.
- Organize fundraising campaigns.
- Record donations and track their statuses.
- Record campaign-related expenses.
- Calculate campaign collection and expense summaries.
- Maintain reliable financial records and enforce data integrity.

## Planned Features

- Administrator registration and login.
- Donor registration, login, and profile management.
- Campaign creation and management.
- Donation recording and status tracking.
- Expense recording and validation against available campaign funds.
- Campaign-wise financial summaries and reports.

Features will be implemented and verified in later development phases.

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, JavaScript (JSX) |
| Backend | Java, Spring Boot, Maven |
| Database | MySQL 8 |
| Version control | Git and GitHub |

## Database Overview

The approved initial database design contains five main entities:

| Entity | Purpose |
|---|---|
| Admin | Stores administrator information |
| Donor | Stores donor information |
| Campaign | Stores fundraising campaign information |
| Donation | Records donations made by donors to campaigns |
| Expense | Records expenses associated with campaigns and administrators |

See [`docs/database_design.md`](docs/database_design.md) and [`docs/er_diagram.md`](docs/er_diagram.md) for the detailed design.

## Repository Structure

```text
ngo-donation-management-system/
├── docs/
│   ├── project_overview.md
│   ├── requirements.md
│   ├── database_design.md
│   ├── er_diagram.md
│   └── api_documentation.md
├── database/       # Planned: MySQL scripts (Phase 2)
├── backend/        # Planned: Spring Boot application (Phase 3)
├── frontend/       # Planned: React application (Phase 4)
├── README.md
└── .gitignore
```

Folders marked as planned may not exist yet.

## Documentation

- [Project Overview](docs/project_overview.md)
- [Requirements](docs/requirements.md)
- [Database Design](docs/database_design.md)
- [ER Diagram](docs/er_diagram.md)
- [API Documentation](docs/api_documentation.md)

## Development Roadmap

| Phase | Work | Status |
|---|---|---|
| 1 | Project planning and database design | Complete |
| 2 | MySQL database and SQL implementation | Planned |
| 3 | Spring Boot backend development | Planned |
| 4 | React frontend development | Planned |
| 5 | Frontend-backend integration | Planned |
| 6 | Testing and final documentation | Planned |

## Getting Started

The application is not yet implemented, so there are no application startup commands at this stage.

### Planned setup

1. Install MySQL 8.
2. Install a compatible Java Development Kit and Maven.
3. Install Node.js and npm.
4. Follow the setup instructions that will be added for the database, backend, and frontend in their respective phases.

## Financial Data Rules

The database design specifies that:

- Monetary values use `DECIMAL(12,2)`.
- Only donations with `COMPLETED` status contribute to collected funds.
- `PENDING`, `FAILED`, and `REFUNDED` donations are excluded from collected totals.
- Campaign totals are calculated from transaction records.
- Donation and expense history must be preserved.

## Academic Project

This project is being developed for academic learning and demonstration of DBMS concepts, including relational database design, normalization, primary and foreign keys, constraints, SQL queries, transactions, and database-backed application development.

## Security Note

Do not commit passwords, API keys, private environment files, real donor personal information, or production database credentials. Use sample data for demonstrations.
