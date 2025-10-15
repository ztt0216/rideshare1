# 🚀 RideShare System Startup Guide

## Prerequisites Check

### 1. Check if ports are free
```powershell
# Check port 8080 (Backend)
netstat -ano | findstr :8080

# Check port 3000 (Frontend)
netstat -ano | findstr :3000

# If ports are occupied, kill the process:
taskkill /PID <ProcessID> /F
```

### 2. Database Status
- Database is hosted on Render (PostgreSQL 17)
- Connection string is configured in `DatabaseConfig.java`
- No local database setup needed

---

## Step 1: Start Backend Server (Port 8080)

### Option A: Using PowerShell

1. Open PowerShell in project root directory:
```powershell
cd d:\learning\2025sm2\SWEN90007\rideshare1
```

2. Start backend server:
```powershell
C:\apache-maven-3.9.11\bin\mvn.cmd tomcat7:run
```

3. Wait for success message:
```
INFO: Starting ProtocolHandler ["http-bio-8080"]
```

4. Test backend is running:
```powershell
# Open browser and visit:
http://localhost:8080/api/users
```

### Option B: Using VS Code Terminal

1. Open Terminal in VS Code (Ctrl + `)
2. Run:
```powershell
mvn tomcat7:run
```

---

## Step 2: Start Frontend (Port 3000)

### 1. Open NEW Terminal/PowerShell window

```powershell
cd d:\learning\2025sm2\SWEN90007\rideshare1\frontend
```

### 2. Install dependencies (first time only)
```powershell
npm install
```

### 3. Start frontend development server
```powershell
npm start
```

4. Browser will automatically open at:
```
http://localhost:3000
```

---

## Step 3: Test the Application

### Create Test Accounts

#### Rider Account
1. Go to http://localhost:3000/register
2. Fill in:
   - Name: Alice
   - Email: alice@example.com
   - Password: password123
   - Role: **Rider**
3. Click "Register"

#### Driver Account
1. Go to http://localhost:3000/register
2. Fill in:
   - Name: Bob
   - Email: bob@example.com
   - Password: password123
   - Role: **Driver**
3. Click "Register"

### Test Rider Flow

1. **Login** as Alice (alice@example.com)
2. **Add money** to wallet (e.g., $500)
3. **Request a ride**:
   - Pickup: 123 Collins St, Melbourne VIC 3000
   - Destination: Melbourne Airport, VIC 3045
   - Expected fare: $60
4. **View ride** in "My Rides" section
5. Wait for driver to accept

### Test Driver Flow

1. **Open incognito window** (Ctrl + Shift + N)
2. **Login** as Bob (bob@example.com)
3. **View available rides** - Should see Alice's ride
4. **Accept ride** - Click "Accept Ride" button
5. **Start trip** - Click "Start Trip" button
6. **Complete ride** - Click "Complete Ride" button
7. **Check balance** - Should receive $60

### Test Rider Side Again

1. **Back to Alice's window**
2. **Refresh** or check "My Rides"
3. **Verify** ride status changed to "Completed"
4. **Check wallet** - Should have $440 ($500 - $60)

---

## Troubleshooting

### Backend won't start

#### Error: "Address already in use: bind <null>:8080"

**Solution:**
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace <PID> with actual process ID)
taskkill /PID <PID> /F

# Try starting backend again
C:\apache-maven-3.9.11\bin\mvn.cmd tomcat7:run
```

#### Error: "Failed to initialize database"

**Solution:**
1. Check internet connection (database is on Render)
2. Run database initializer:
```powershell
C:\apache-maven-3.9.11\bin\mvn.cmd exec:java -Dexec.mainClass=com.rideshare.util.DatabaseInitializer
```

### Frontend Issues

#### Error: "npm: command not found"

**Solution:**
- Install Node.js from https://nodejs.org/
- Restart terminal after installation

#### Error: "Failed to compile"

**Solution:**
```powershell
# Clean and reinstall dependencies
cd frontend
Remove-Item node_modules -Recurse -Force
Remove-Item package-lock.json -Force
npm install
npm start
```

#### Error: "Port 3000 is already in use"

**Solution:**
```powershell
# Find and kill process on port 3000
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Or use different port
$env:PORT=3001
npm start
```

### API Connection Issues

#### Error: "Network Error" or "Failed to fetch"

**Checklist:**
1. ✅ Backend is running on http://localhost:8080
2. ✅ Frontend is running on http://localhost:3000
3. ✅ Check browser console for CORS errors
4. ✅ Verify `package.json` has `"proxy": "http://localhost:8080"`

---

## System Architecture

```
┌─────────────────────────────────────────┐
│         User's Browser                   │
│                                          │
│    http://localhost:3000                 │
│    (React Frontend)                      │
└─────────────┬───────────────────────────┘
              │
              │ API Calls (Proxied)
              ▼
┌─────────────────────────────────────────┐
│    Backend Server                        │
│                                          │
│    http://localhost:8080                 │
│    (Java Servlets + Tomcat)              │
└─────────────┬───────────────────────────┘
              │
              │ JDBC
              ▼
┌─────────────────────────────────────────┐
│    PostgreSQL Database                   │
│                                          │
│    dpg-***-a.oregon-postgres.render.com  │
│    (Hosted on Render)                    │
└─────────────────────────────────────────┘
```

---

## Quick Command Reference

### Backend
```powershell
# Start backend
C:\apache-maven-3.9.11\bin\mvn.cmd tomcat7:run

# Stop backend
Ctrl + C

# Test backend
curl http://localhost:8080/api/users
```

### Frontend
```powershell
# Start frontend
cd frontend
npm start

# Stop frontend
Ctrl + C

# Build for production
npm run build
```

### Database
```powershell
# Initialize database tables
C:\apache-maven-3.9.11\bin\mvn.cmd exec:java -Dexec.mainClass=com.rideshare.util.DatabaseInitializer
```

---

## API Endpoints Reference

### Users
- `POST /api/users` - Register new user
- `POST /api/users/login` - Login
- `GET /api/users/{id}` - Get user details
- `POST /api/users/{id}/wallet` - Update wallet balance

### Rides
- `POST /api/rides` - Request ride (Rider)
- `GET /api/rides` - Get available rides (Driver)
- `POST /api/rides/{id}/accept` - Accept ride (Driver)
- `POST /api/rides/{id}/start` - Start trip (Driver)
- `POST /api/rides/{id}/complete` - Complete ride (Driver)
- `POST /api/rides/{id}/cancel` - Cancel ride (Rider)
- `GET /api/rides/rider/{riderId}` - Get rider history
- `GET /api/rides/driver/{driverId}` - Get driver history

---

## Next Steps

After successful testing:

1. ✅ Test concurrent ride requests
2. ✅ Test multiple drivers accepting same ride (optimistic lock)
3. ✅ Test insufficient wallet balance
4. ✅ Test ride cancellation
5. ✅ Review code in VS Code
6. ✅ Check database records on Render

---

## Support

For issues or questions:
1. Check this guide's Troubleshooting section
2. Review main `README.md`
3. Check frontend `README.md` in `frontend/` directory
4. Review backend error logs in terminal

---

**Happy Testing! 🎉**
