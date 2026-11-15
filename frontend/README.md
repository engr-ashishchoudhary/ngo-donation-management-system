# NGO Donation Management System — Frontend

React + Vite (JSX) client for the existing Spring Boot backend.

## Run

```bash
# 1. Backend (from D:\project\ngo-donation-management-system\backend)
#    Requires env vars: DB_USERNAME, DB_PASSWORD, JWT_SECRET (>= 32 chars),
#    INITIAL_SUPER_ADMIN_PASSWORD (first run, to replace the seeded placeholder hash)
mvn spring-boot:run

# 2. Frontend (from D:\project\ngo-donation-management-system\frontend)
copy .env.example .env      # macOS/Linux: cp .env.example .env
npm install
npm run dev                 # http://localhost:5173
npm run build               # production build into dist/
```

The backend allows CORS from `http://localhost:5173` and `http://localhost:3000` by default.

## Routes

| Path | Access |
|---|---|
| `/` | Portal chooser |
| `/admin/login`, `/donor/login`, `/donor/register` | Public (signed-in users are redirected) |
| `/admin/dashboard \| campaigns \| donations \| expenses \| donors \| profile` | ADMIN, SUPER_ADMIN |
| `/admin/admins` | SUPER_ADMIN only |
| `/donor/dashboard \| campaigns \| donations \| profile` | DONOR only |

## Structure

`src/api` (Axios + per-domain modules) · `src/context` (auth, toasts) · `src/routes` (guards) ·
`src/components/{common,forms,tables,layout,campaigns,donations}` · `src/layouts` · `src/pages/{auth,admin,donor}` ·
`src/utils` · `src/styles`
