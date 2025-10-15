import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, Navigate } from 'react-router-dom';
import Login from './components/Login.jsx';
import Register from './components/Register.jsx';
import RiderDashboard from './components/RiderDashboard.jsx';
import DriverDashboard from './components/DriverDashboard.jsx';
import './App.css';

function App() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Check if user is logged in (from localStorage)
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
  }, []);

  const handleLogin = (userData) => {
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user');
  };

  return (
    <Router>
      <div className="App">
        <nav className="navbar">
          <div className="navbar-content">
            <h1>🚗 RideShare</h1>
            <div className="navbar-links">
              {!user ? (
                <>
                  <Link to="/login">Login</Link>
                  <Link to="/register">Register</Link>
                </>
              ) : (
                <>
                  <span style={{ color: '#ecf0f1' }}>
                    Welcome, {user.name} ({user.role === 'RIDER' ? 'Rider' : 'Driver'})
                  </span>
                  <span style={{ color: '#ecf0f1' }}>
                    💰 Balance: ${user.walletBalance || 0}
                  </span>
                  <button onClick={handleLogout}>Logout</button>
                </>
              )}
            </div>
          </div>
        </nav>

        <div className="container">
          <Routes>
            <Route 
              path="/login" 
              element={user ? <Navigate to="/" /> : <Login onLogin={handleLogin} />} 
            />
            <Route 
              path="/register" 
              element={user ? <Navigate to="/" /> : <Register />} 
            />
            <Route 
              path="/" 
              element={
                !user ? (
                  <Navigate to="/login" />
                ) : user.role === 'RIDER' ? (
                  <RiderDashboard user={user} onUpdateUser={handleLogin} />
                ) : (
                  <DriverDashboard user={user} onUpdateUser={handleLogin} />
                )
              } 
            />
          </Routes>
        </div>
      </div>
    </Router>
  );
}

export default App;
