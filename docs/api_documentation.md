# API Documentation — NGO Donation Management System

> **Status:** Planned — Not yet implemented.
> This document describes the REST API that will be built in Phase 3 (Backend Development).

---

## General Conventions

| Convention | Detail |
|-----------|--------|
| Base URL | `http://localhost:8080/api` |
| Format | JSON (`Content-Type: application/json`) |
| Authentication | Bearer JWT token in `Authorization` header |
| Timestamps | ISO 8601 format (`2026-11-14T11:00:00`) |
| Monetary amounts | Decimal string, e.g. `"10000.00"` |
| Success codes | `200 OK`, `201 Created`, `204 No Content` |
| Error codes | `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity` |

### Standard Error Response Body
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Donation amount must be greater than 0.",
  "timestamp": "2026-11-14T11:00:00"
}
```

---

## 1. Auth Endpoints

### 1.1 Admin Register
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/auth/admin/register` |
| **Auth** | None (or SUPER_ADMIN JWT for production hardening) |

**Request Body:**
```json
{
  "name": "Priya Sharma",
  "email": "admin@ngo.org",
  "password": "secret123",
  "role": "ADMIN"
}
```
**Response `201 Created`:**
```json
{
  "adminId": 1,
  "name": "Priya Sharma",
  "email": "admin@ngo.org",
  "role": "ADMIN",
  "createdAt": "2026-11-14T11:00:00"
}
```

---

### 1.2 Admin Login
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/auth/admin/login` |
| **Auth** | None |

**Request Body:**
```json
{
  "email": "admin@ngo.org",
  "password": "secret123"
}
```
**Response `200 OK`:**
```json
{
  "token": "<jwt-string>",
  "adminId": 1,
  "name": "Priya Sharma",
  "role": "ADMIN"
}
```

---

### 1.3 Donor Register
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/auth/donor/register` |
| **Auth** | None |

**Request Body:**
```json
{
  "name": "Rahul Verma",
  "email": "rahul@email.com",
  "password": "mypassword",
  "phone": "9876543210",
  "address": "12 MG Road, Bengaluru"
}
```
**Response `201 Created`:**
```json
{
  "donorId": 5,
  "name": "Rahul Verma",
  "email": "rahul@email.com",
  "createdAt": "2026-11-14T11:00:00"
}
```

---

### 1.4 Donor Login
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/auth/donor/login` |
| **Auth** | None |

**Request Body:**
```json
{
  "email": "rahul@email.com",
  "password": "mypassword"
}
```
**Response `200 OK`:**
```json
{
  "token": "<jwt-string>",
  "donorId": 5,
  "name": "Rahul Verma"
}
```

---

## 2. Campaign Endpoints

### 2.1 Get All Active Campaigns
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/campaigns` |
| **Auth** | Optional (public read) |

**Query Params (optional):** `status=ACTIVE`, `page=0`, `size=10`

**Response `200 OK`:**
```json
[
  {
    "campaignId": 1,
    "name": "Clean Water for Villages",
    "description": "...",
    "goalAmount": "500000.00",
    "totalCollected": "320000.00",
    "totalSpent": "50000.00",
    "availableFunds": "270000.00",
    "status": "ACTIVE",
    "startDate": "2026-10-01",
    "endDate": "2026-12-31"
  }
]
```

---

### 2.2 Get Single Campaign
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/campaigns/{campaignId}` |
| **Auth** | Optional |

**Response `200 OK`:** Same structure as above (single object).
**Response `404`:** Campaign not found.

---

### 2.3 Create Campaign
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/campaigns` |
| **Auth** | Admin JWT required |

**Request Body:**
```json
{
  "name": "School Supplies Drive",
  "description": "Provide stationery to 500 children.",
  "goalAmount": "150000.00",
  "startDate": "2026-11-15",
  "endDate": "2027-01-15"
}
```
**Response `201 Created`:**
```json
{
  "campaignId": 4,
  "name": "School Supplies Drive",
  "goalAmount": "150000.00",
  "status": "ACTIVE",
  "createdAt": "2026-11-14T11:00:00"
}
```

---

### 2.4 Update Campaign
| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `/api/campaigns/{campaignId}` |
| **Auth** | Admin JWT required |

**Request Body:** Any updatable field (`name`, `description`, `goalAmount`, `endDate`, `status`).

**Response `200 OK`:** Updated campaign object.
**Response `404`:** Not found.

---

### 2.5 Close Campaign
| Field | Value |
|-------|-------|
| **Method** | `PATCH` |
| **URL** | `/api/campaigns/{campaignId}/close` |
| **Auth** | Admin JWT required |

**Response `200 OK`:**
```json
{
  "campaignId": 4,
  "status": "CLOSED"
}
```

---

## 3. Donation Endpoints

### 3.1 Get All Donations (Admin)
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/donations` |
| **Auth** | Admin JWT required |

**Query Params (optional):** `campaignId=1`, `status=COMPLETED`, `page=0`, `size=20`

**Response `200 OK`:**
```json
[
  {
    "donationId": 12,
    "donorId": 5,
    "donorName": "Rahul Verma",
    "campaignId": 1,
    "campaignName": "Clean Water for Villages",
    "amount": "5000.00",
    "paymentMethod": "UPI",
    "status": "COMPLETED",
    "transactionRef": "UPI20261114001",
    "donatedAt": "2026-11-14T10:30:00"
  }
]
```

---

### 3.2 Get My Donations (Donor)
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/donations/my` |
| **Auth** | Donor JWT required |

**Response `200 OK`:** List of the authenticated donor's donations (same structure as 3.1).

---

### 3.3 Get Single Donation
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/donations/{donationId}` |
| **Auth** | Admin JWT **or** the owning Donor JWT |

**Response `200 OK`:** Single donation object (used by the frontend receipt/detail page).
**Response `403`:** Donor attempting to view another donor's donation.
**Response `404`:** Donation not found.

---

### 3.4 Make a Donation
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/donations` |
| **Auth** | Donor JWT required |

**Request Body:**
```json
{
  "campaignId": 1,
  "amount": "1000.00",
  "paymentMethod": "UPI",
  "transactionRef": "UPI20261114099",
  "notes": "Happy to help!"
}
```
**Response `201 Created`:**
```json
{
  "donationId": 15,
  "campaignId": 1,
  "amount": "1000.00",
  "status": "PENDING",
  "donatedAt": "2026-11-14T11:05:00"
}
```
**Response `422`:** Campaign not ACTIVE, or invalid amount.

---

### 3.5 Update Donation Status (Admin)
| Field | Value |
|-------|-------|
| **Method** | `PATCH` |
| **URL** | `/api/donations/{donationId}/status` |
| **Auth** | Admin JWT required |

**Request Body:**
```json
{
  "status": "COMPLETED"
}
```
**Response `200 OK`:** Updated donation object.

---

## 4. Expense Endpoints

### 4.1 Get All Expenses for a Campaign
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/campaigns/{campaignId}/expenses` |
| **Auth** | Admin JWT required |

**Response `200 OK`:**
```json
[
  {
    "expenseId": 3,
    "campaignId": 1,
    "adminId": 1,
    "adminName": "Priya Sharma",
    "description": "Water purifier units",
    "amount": "30000.00",
    "category": "SUPPLIES",
    "expenseDate": "2026-11-10",
    "createdAt": "2026-11-10T14:00:00"
  }
]
```

---

### 4.2 Record an Expense
| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/campaigns/{campaignId}/expenses` |
| **Auth** | Admin JWT required |

**Request Body:**
```json
{
  "description": "Diesel for water trucks",
  "amount": "8000.00",
  "category": "LOGISTICS",
  "expenseDate": "2026-11-14"
}
```
**Response `201 Created`:**
```json
{
  "expenseId": 7,
  "campaignId": 1,
  "amount": "8000.00",
  "category": "LOGISTICS",
  "createdAt": "2026-11-14T11:10:00"
}
```
**Response `422`:** Expense exceeds available campaign funds.

---

## 5. Admin Endpoints

### 5.1 Get All Admins (Super Admin)
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/admin/admins` |
| **Auth** | SUPER_ADMIN JWT required |

**Response `200 OK`:** List of all admin accounts (excluding `password_hash`).

---

### 5.2 Get All Donors (Admin)
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/admin/donors` |
| **Auth** | Admin JWT required |

**Response `200 OK`:** List of all donors (excluding `password_hash`).

---

### 5.3 Get Dashboard Summary (Admin)
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/admin/dashboard` |
| **Auth** | Admin JWT required |

**Response `200 OK`:**
```json
{
  "totalDonorsCount": 42,
  "activeCampaignsCount": 3,
  "totalDonationsCollected": "1250000.00",
  "totalExpensesRecorded": "400000.00"
}
```

---

### 5.4 Get Donor Profile (Donor)
| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/donor/profile` |
| **Auth** | Donor JWT required |

**Response `200 OK`:** Authenticated donor's profile (excluding `password_hash`).

---

### 5.5 Update Donor Profile (Donor)
| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `/api/donor/profile` |
| **Auth** | Donor JWT required |

**Request Body:**
```json
{
  "name": "Rahul Verma",
  "phone": "9123456789",
  "address": "45 Park Street, Mumbai"
}
```
**Response `200 OK`:** Updated donor profile object.

> **Note:** Email and password changes are not in scope for Phase 3 MVP. Only `name`, `phone`, and `address` are updatable here.

---

## 6. Endpoint Summary Table

| # | Method | URL | Role | Description |
|---|--------|-----|------|-------------|
| 1 | POST | `/api/auth/admin/register` | Public | Admin registration |
| 2 | POST | `/api/auth/admin/login` | Public | Admin login |
| 3 | POST | `/api/auth/donor/register` | Public | Donor registration |
| 4 | POST | `/api/auth/donor/login` | Public | Donor login |
| 5 | GET | `/api/campaigns` | Public | List campaigns (filterable by status) |
| 6 | GET | `/api/campaigns/{id}` | Public | Get one campaign with financial totals |
| 7 | POST | `/api/campaigns` | Admin | Create campaign |
| 8 | PUT | `/api/campaigns/{id}` | Admin | Update campaign details |
| 9 | PATCH | `/api/campaigns/{id}/close` | Admin | Close campaign |
| 10 | GET | `/api/donations` | Admin | List all donations (filterable) |
| 11 | GET | `/api/donations/my` | Donor | My donation history |
| 12 | GET | `/api/donations/{id}` | Admin / Donor (own) | Get single donation / receipt |
| 13 | POST | `/api/donations` | Donor | Make a donation |
| 14 | PATCH | `/api/donations/{id}/status` | Admin | Update donation status |
| 15 | GET | `/api/campaigns/{id}/expenses` | Admin | List campaign expenses |
| 16 | POST | `/api/campaigns/{id}/expenses` | Admin | Record an expense |
| 17 | GET | `/api/admin/admins` | SUPER_ADMIN | List all admins |
| 18 | GET | `/api/admin/donors` | Admin | List all donors |
| 19 | GET | `/api/admin/dashboard` | Admin | Dashboard metrics |
| 20 | GET | `/api/donor/profile` | Donor | Donor's own profile |
| 21 | PUT | `/api/donor/profile` | Donor | Update donor profile |

---

*Document version: 1.1 | Phase: 1 — Planning (reviewed & corrected)*
