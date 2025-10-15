# Rideshare Backend – PART3 (Maven / WAR / Servlet 6 / H2 / JDBC / UoW / Locks)

## Build & Run
1. `mvn clean package`
2. Deploy `target/rideshare-backend.war` to Tomcat/Jetty
3. Test:
   - `GET /api/health` → `{"status":"OK"}`
   - Seed users: rider@example.com / driver@example.com (password: `password`)

## Endpoints
- `POST /api/auth/login` {email,password}
- Rider: `POST /api/rides/request` {pickupPostcode,destPostcode} ; `POST /api/rides/cancel` {rideId}
- Driver: `POST /api/driver/accept|begin|complete` {rideId}
- Wallet: `GET /api/wallet` ; `POST /api/wallet` {amountCents}
- Availability: `POST /api/availability` {dayOfWeek,start,end}
- History: `GET /api/history?role=driver`

## Design
- Layered: controller → service → persistence (mappers) → DB
- Patterns: Data Mapper, Unit of Work (batched ops), Pessimistic Locks (SELECT ... FOR UPDATE)
- Concurrency:
  - Cancel vs Accept: lock ride row; check state before transition.
  - Top-up vs Payment: lock both wallets; atomic debit/credit + payment + status update.
- DB: H2 in-memory (PostgreSQL mode) with schema + seed auto-loaded on startup.
