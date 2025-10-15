# RideShare Frontend - React Application

## Features

### Rider Features
- ✅ User Registration/Login
- ✅ Wallet Management (View Balance, Add Money)
- ✅ Request Ride Service (Auto-calculate fare)
- ✅ View Ride History
- ✅ Cancel Ride (REQUESTED status only)
- ✅ Real-time Ride Status Updates

### Driver Features
- ✅ User Registration/Login
- ✅ View Available Rides (Auto-refresh)
- ✅ Accept Rides (Optimistic Lock Protection)
- ✅ Start Trip
- ✅ Complete Ride (Auto Payment)
- ✅ View Ride History
- ✅ Earnings Statistics

## Tech Stack

- **React 18** - UI Framework
- **React Router 6** - Routing
- **Axios** - HTTP Client
- **CSS3** - Styling

## Installation

### 1. Install Dependencies
```bash
cd frontend
npm install
```

### 2. Start Backend Server
Ensure backend server is running on port 8080:
```powershell
# In rideshare1 root directory
mvn tomcat7:run
```

### 3. Start Frontend Development Server
```bash
npm start
```

Application will automatically open at http://localhost:3000

## Project Structure

```
frontend/
├── public/
│   └── index.html          # HTML Template
├── src/
│   ├── components/         # React Components
│   │   ├── Login.js       # Login Page
│   │   ├── Register.js    # Registration Page
│   │   ├── RiderDashboard.js   # Rider Dashboard
│   │   └── DriverDashboard.js  # Driver Dashboard
│   ├── services/
│   │   └── api.js         # API Service Layer
│   ├── App.js             # Main App Component
│   ├── App.css            # App Styles
│   ├── index.js           # Entry Point
│   └── index.css          # Global Styles
├── package.json           # Dependencies
└── README.md             # This File
```

## User Guide

### First Time Use

1. **Register an Account**
   - Visit http://localhost:3000/register
   - Select role (Rider/Driver)
   - Fill in registration information

2. **Login**
   - Use registered email and password to login
   - System redirects to appropriate dashboard based on role

### Rider Workflow

1. **Add Money to Wallet**
   - Enter amount in "My Wallet" card
   - Click "Add Money" button

2. **Request a Ride**
   - Click "Request Ride" button
   - Fill in pickup location and destination (must include postcode)
   - System automatically calculates fare
   - Confirm request

3. **View Rides**
   - View all rides in "My Rides" section
   - Wait for driver to accept
   - Can cancel rides with REQUESTED status

### Driver Workflow

1. **View Available Rides**
   - Check all pending rides in "Available Rides" tab
   - Page auto-refreshes every 10 seconds
   - View ride details and earnings

2. **Accept Ride**
   - Click "Accept Ride" button
   - If multiple drivers accept simultaneously, only one succeeds (optimistic lock)

3. **Complete Ride**
   - After accepting, click "Start Trip"
   - Upon arrival at destination, click "Complete Ride"
   - Payment automatically credited to account

4. **View History**
   - Check all ride history in "My Rides" tab
   - View earnings statistics

## Fare Information

System automatically calculates fare based on destination postcode:

- **Airport (3045)**: $60
- **City (3000-3299)**: $40
- **Regional (3300-3999)**: $220
- **Interstate (non-3xxx)**: $500

## API Configuration

Frontend connects to backend API via proxy configuration:

```json
"proxy": "http://localhost:8080"
```

All API requests are automatically proxied to backend server.

## Development Configuration

### Port Configuration
- Frontend: http://localhost:3000
- Backend: http://localhost:8080

### API Endpoints
- Users: `/api/users`
- Rides: `/api/rides`
- Driver Availability: `/api/drivers/availability`

## Build for Production

```bash
npm run build
```

Build files will be generated in `build/` directory.

## Troubleshooting

### Port Conflict
If port 3000 is occupied:
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <process_id> /F

# Or set different port
set PORT=3001 && npm start
```

### API Connection Failed
1. Confirm backend server is running on port 8080
2. Check if backend API responds normally
3. Check browser console for errors

### Style Issues
Clear browser cache or test in incognito mode.

## Test Accounts

Create test accounts using backend test script:
```powershell
.\test-complete.ps1
```

This creates:
- Rider: alice@example.com / password123 (Balance $500)
- Driver: bob@example.com / password123

## Feature Highlights

### User Experience
- 🎨 Modern UI Design
- 📱 Responsive Layout
- ⚡ Real-time Data Refresh
- 🔔 Friendly Error Messages
- 💫 Smooth Interactions

### Technical Features
- 🔐 Client Session Management (localStorage)
- 🔄 Auto-refresh (Driver 10s)
- 🎯 Precise Error Handling
- 📊 Real-time Statistics
- 🚀 Optimized API Calls

## Future Enhancements

- [ ] Real-time Location Tracking (Google Maps)
- [ ] WebSocket Push Notifications
- [ ] Driver Availability Settings Interface
- [ ] Ride Rating System
- [ ] Payment History Details
- [ ] User Profile Editing
- [ ] Multi-language Support

## License

This project is for educational purposes.

## Contact

For questions, please refer to main project README.md or submit an Issue.
