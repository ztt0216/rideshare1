# Test Ride Creation After Database Fix

## What Was Fixed
1. **Missing Database Tables**: Added automatic database initialization
   - `DatabaseInitializerListener` runs on app startup
   - Checks if tables exist
   - Creates all required tables if missing:
     - `users` (already existed from previous registration test)
     - `rides` (NEW - was missing!)
     - `payments` (NEW - was missing!)
     - `driver_availability`
     - `ride_status` enum type

2. **Database Initialization**: Added `@WebListener` that runs when Tomcat starts
   - Located in: `src/main/java/com/rideshare/config/DatabaseInitializerListener.java`
   - Automatically detects if tables exist
   - Creates tables only if needed
   - Shows detailed logs during initialization

## Test Steps After Deployment

### Step 1: Wait for Render Deployment
- Go to: https://dashboard.render.com
- Find service: `rideshare-backend1`
- Wait for status: **Live** (green)
- Check logs for database initialization messages

### Step 2: Test Registration (Should Still Work)
```powershell
$body = @{
    name="RiderTest"
    email="rider@test.com"
    password="test123"
    role="RIDER"
} | ConvertTo-Json

Invoke-WebRequest -Uri "https://rideshare-backend1.onrender.com/api/users" `
    -Method POST -Body $body -ContentType "application/json"
```

**Expected**: 201 Created with user data including `id` and `walletBalance=0`

### Step 3: Add Money to Wallet
```powershell
# Assuming user ID is 2 (from previous registration)
$body = @{amount=100} | ConvertTo-Json

Invoke-WebRequest -Uri "https://rideshare-backend1.onrender.com/api/users/2/wallet" `
    -Method POST -Body $body -ContentType "application/json"
```

**Expected**: 200 OK, wallet balance updated to $100

### Step 4: Request a Ride (City - $40)
```powershell
$body = @{
    riderId=2
    pickupLocation="3000"
    destination="3100"
} | ConvertTo-Json

Invoke-WebRequest -Uri "https://rideshare-backend1.onrender.com/api/rides" `
    -Method POST -Body $body -ContentType "application/json"
```

**Expected**: 201 Created
```json
{"rideId":1,"message":"Ride requested successfully"}
```

### Step 5: Request Airport Ride ($60)
```powershell
$body = @{
    riderId=2
    pickupLocation="3000"
    destination="3045"
} | ConvertTo-Json

Invoke-WebRequest -Uri "https://rideshare-backend1.onrender.com/api/rides" `
    -Method POST -Body $body -ContentType "application/json"
```

**Expected**: 201 Created
```json
{"rideId":2,"message":"Ride requested successfully"}
```

### Step 6: Test in Browser
1. Open: http://localhost:3000
2. Register new user (or login if already registered)
3. Add money to wallet (at least $60)
4. Create ride:
   - Pickup: 3000
   - Destination: 3045 (Airport - $60)
5. Click "Confirm Request"
6. **Should succeed!** ✅

## Fare Calculation Reference
- **City (3000-3299)**: $40
- **Airport (3045)**: $60
- **Regional (3300-3999)**: $220
- **Interstate (Other)**: $500

## Files Modified
1. `src/main/java/com/rideshare/config/DatabaseInitializerListener.java` - NEW
   - Automatic database initialization on startup
2. `src/main/java/com/rideshare/config/DatabaseConfig.java`
   - Fixed port parsing (use 5432 if not specified)
   - Added SSL mode for PostgreSQL
3. `src/main/resources/db/migration/V3__Create_rides_table.sql` - NEW
4. `src/main/resources/db/migration/V4__Create_payments_table.sql` - NEW

## Database Schema Created
```sql
-- Users table (already existed)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    wallet_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

-- Rides table (NOW CREATED!)
CREATE TABLE rides (
    id SERIAL PRIMARY KEY,
    rider_id BIGINT NOT NULL,
    driver_id BIGINT,
    pickup_location VARCHAR(500) NOT NULL,
    destination VARCHAR(500) NOT NULL,
    fare DECIMAL(10,2) NOT NULL,
    status ride_status NOT NULL DEFAULT 'REQUESTED',
    requested_time TIMESTAMP NOT NULL,
    completed_time TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (rider_id) REFERENCES users(id),
    FOREIGN KEY (driver_id) REFERENCES users(id)
);

-- Payments table (NOW CREATED!)
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    ride_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_time TIMESTAMP NOT NULL,
    FOREIGN KEY (ride_id) REFERENCES rides(id)
);
```

## Troubleshooting
If ride creation still fails:
1. Check Render logs for errors
2. Verify database tables were created (should see initialization logs)
3. Check user wallet balance is sufficient
4. Verify user exists in database
