# Quick Start Guide - Ride Management System

## 🚀 Quick Setup & Test (5 minutes)

### One-Command Test
```powershell
# This will do everything: initialize DB, compile, start server, and test
.\test-complete.ps1
```

### Manual Step-by-Step

#### 1. Initialize Database
```powershell
C:\apache-maven-3.9.11\bin\mvn -f d:\learning\2025sm2\SWEN90007\rideshare1\pom.xml exec:java -Dexec.mainClass="com.rideshare.util.DatabaseInitializer"
```

#### 2. Compile & Start Server
```powershell
# Clean and compile
C:\apache-maven-3.9.11\bin\mvn -f d:\learning\2025sm2\SWEN90007\rideshare1\pom.xml clean compile

# Stop any running server
taskkill /F /IM java.exe

# Start Tomcat (in new window)
C:\apache-maven-3.9.11\bin\mvn -f d:\learning\2025sm2\SWEN90007\rideshare1\pom.xml tomcat7:run
```

#### 3. Create Test Users
```powershell
# Create Rider
Invoke-RestMethod -Uri http://localhost:8080/api/users -Method Post -Body (@{name="Alice";email="alice@example.com";password="pass123";role="RIDER"} | ConvertTo-Json) -ContentType "application/json"

# Add $500 to rider wallet (user ID 1)
Invoke-RestMethod -Uri http://localhost:8080/api/users/1/wallet -Method Post -Body (@{amount=500} | ConvertTo-Json) -ContentType "application/json"

# Create Driver
Invoke-RestMethod -Uri http://localhost:8080/api/users -Method Post -Body (@{name="Bob";email="bob@example.com";password="pass123";role="DRIVER"} | ConvertTo-Json) -ContentType "application/json"
```

#### 4. Test Ride Workflow
```powershell
# Request a ride (Metro fare: $40)
$ride = Invoke-RestMethod -Uri http://localhost:8080/api/rides -Method Post -Body (@{riderId=1;pickupLocation="123 Collins St, Melbourne VIC 3000";destination="456 Bourke St, Melbourne VIC 3000"} | ConvertTo-Json) -ContentType "application/json"

# Driver accepts ride
Invoke-RestMethod -Uri "http://localhost:8080/api/rides/$($ride.rideId)/accept" -Method Post -Body (@{driverId=2} | ConvertTo-Json) -ContentType "application/json"

# Driver starts ride
Invoke-RestMethod -Uri "http://localhost:8080/api/rides/$($ride.rideId)/start" -Method Post -Body (@{driverId=2} | ConvertTo-Json) -ContentType "application/json"

# Driver completes ride (payment processed!)
Invoke-RestMethod -Uri "http://localhost:8080/api/rides/$($ride.rideId)/complete" -Method Post -Body (@{driverId=2} | ConvertTo-Json) -ContentType "application/json"

# Check wallets
Invoke-RestMethod -Uri http://localhost:8080/api/users/1 -Method Get  # Rider: $460
Invoke-RestMethod -Uri http://localhost:8080/api/users/2 -Method Get  # Driver: $40
```

## 📋 API Endpoints

### Ride Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/rides | Request a new ride |
| GET | /api/rides | Get available rides (for drivers) |
| GET | /api/rides/{id} | Get ride details |
| POST | /api/rides/{id}/accept | Driver accepts ride |
| POST | /api/rides/{id}/start | Driver starts ride |
| POST | /api/rides/{id}/complete | Driver completes ride (payment) |
| POST | /api/rides/{id}/cancel | Rider cancels ride |
| GET | /api/rides/rider/{riderId} | Get rider history |
| GET | /api/rides/driver/{driverId} | Get driver history |

### User Management (Already Implemented)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/users | Register user |
| POST | /api/users/login | Login user |
| GET | /api/users/{id} | Get user details |
| POST | /api/users/{id}/wallet | Update wallet |

## 💰 Fare Calculation Rules

| Zone | Postcode | Fare |
|------|----------|------|
| Airport | 3045 | $60 |
| Interstate | Non-3xxx | $500 |
| Regional | 3300-3999 | $220 |
| Metro | 3000-3299 | $40 |

## 🔄 Ride State Machine

```
REQUESTED ──accept──> ACCEPTED ──start──> ENROUTE ──complete──> COMPLETED
    │                                                                ^
    │                                                                │
    └──cancel──> CANCELLED                                Payment Processed
```

## 🧪 Test Scenarios

### Scenario 1: Metro Ride ($40)
```json
POST /api/rides
{
  "riderId": 1,
  "pickupLocation": "123 Collins St, Melbourne VIC 3000",
  "destination": "456 Bourke St, Melbourne VIC 3000"
}
```

### Scenario 2: Airport Ride ($60)
```json
POST /api/rides
{
  "riderId": 1,
  "pickupLocation": "123 Collins St, Melbourne VIC 3000",
  "destination": "Melbourne Airport, VIC 3045"
}
```

### Scenario 3: Regional Ride ($220)
```json
POST /api/rides
{
  "riderId": 1,
  "pickupLocation": "123 Collins St, Melbourne VIC 3000",
  "destination": "456 Main St, Bendigo VIC 3500"
}
```

### Scenario 4: Interstate Ride ($500)
```json
POST /api/rides
{
  "riderId": 1,
  "pickupLocation": "123 Collins St, Melbourne VIC 3000",
  "destination": "789 George St, Sydney NSW 2000"
}
```

## 🔒 Concurrency Features

### Optimistic Locking (Ride Acceptance)
Multiple drivers can try to accept the same ride, but only the first one succeeds:
```powershell
# Driver 1 accepts
Invoke-RestMethod -Uri http://localhost:8080/api/rides/1/accept -Method Post -Body (@{driverId=2} | ConvertTo-Json) -ContentType "application/json"
# Success: {"message": "Ride accepted successfully"}

# Driver 2 tries to accept same ride
Invoke-RestMethod -Uri http://localhost:8080/api/rides/1/accept -Method Post -Body (@{driverId=3} | ConvertTo-Json) -ContentType "application/json"
# Error: {"error": "Optimistic lock failure: ride has been modified by another transaction"}
```

### Pessimistic Locking (Wallet Operations)
All wallet updates during payment are atomic:
- Deduct from rider wallet
- Credit to driver wallet
- Create payment record
- Update ride status

**If any step fails, entire transaction rolls back!**

## ⚠️ Common Issues & Solutions

### Issue 1: Port 8080 in use
```powershell
taskkill /F /IM java.exe
```

### Issue 2: Database connection failed
Check DATABASE_URL in DatabaseConfig.java:
```
jdbc:postgresql://dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com/rideshare1
```

### Issue 3: Insufficient wallet balance
```json
{"error": "Insufficient wallet balance. Required: $60.00, Available: $50.00"}
```
Solution: Add money to wallet first
```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/users/1/wallet -Method Post -Body (@{amount=100} | ConvertTo-Json) -ContentType "application/json"
```

## 📊 Database Schema

Tables created by DatabaseInitializer:
- ✅ users (id, name, email, password, role, wallet_balance)
- ✅ driver_availability (id, driver_id, day_of_week, start_time, end_time)
- ✅ rides (id, rider_id, driver_id, pickup_location, destination, fare, status, requested_time, completed_time, version)
- ✅ payments (id, ride_id, amount, payment_time)

Types:
- ✅ ride_status ENUM (REQUESTED, ACCEPTED, ENROUTE, COMPLETED, CANCELLED)

## 🎯 What's Working

✅ Full ride workflow (request → accept → start → complete)  
✅ Fare calculation with postcode zones  
✅ Payment processing (wallet deduction/credit)  
✅ Optimistic locking for concurrent ride acceptance  
✅ Transaction management for payment atomicity  
✅ Rider and driver history  
✅ Ride cancellation  
✅ Melbourne timezone support  
✅ State machine validation  
✅ RESTful API  

## 🚧 Next Features (Future)

- Driver availability integration
- Ride matching algorithm
- Real-time notifications
- Rating system
- Trip analytics
- Payment refunds

## 📞 Support

If tests fail, check:
1. Server is running: `http://localhost:8080/api/rides`
2. Database is initialized: Check tables exist
3. Users exist with sufficient balance
4. Logs in Tomcat console for errors

Happy Testing! 🎉
