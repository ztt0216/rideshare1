import React, { useState, useEffect } from 'react';
import { 
  getAvailableRides, 
  getDriverHistory, 
  acceptRide, 
  startRide, 
  completeRide,
  getUser
} from '../services/api';

function DriverDashboard({ user, onUpdateUser }) {
  const [availableRides, setAvailableRides] = useState([]);
  const [myRides, setMyRides] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeTab, setActiveTab] = useState('available'); // 'available' or 'history'

  useEffect(() => {
    loadData();
    // Auto refresh every 10 seconds
    const interval = setInterval(loadData, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadData = async () => {
    try {
      const [available, history] = await Promise.all([
        getAvailableRides(),
        getDriverHistory(user.id)
      ]);
      setAvailableRides(available);
      setMyRides(history);
    } catch (err) {
      console.error('Failed to load data:', err);
    }
  };

  const refreshUserData = async () => {
    try {
      const userData = await getUser(user.id);
      onUpdateUser(userData);
    } catch (err) {
      console.error('Failed to refresh user data:', err);
    }
  };

  const handleAcceptRide = async (rideId) => {
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await acceptRide(rideId, user.id);
      setSuccess('Ride accepted successfully!');
      loadData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to accept ride. It may have been taken by another driver');
    } finally {
      setLoading(false);
    }
  };

  const handleStartRide = async (rideId) => {
    try {
      await startRide(rideId, user.id);
      setSuccess('Trip started!');
      loadData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to start trip');
    }
  };

  const handleCompleteRide = async (rideId) => {
    if (!window.confirm('Confirm completion of this ride? Payment will be processed automatically.')) return;
    
    try {
      await completeRide(rideId, user.id);
      setSuccess('Ride completed, payment received!');
      loadData();
      refreshUserData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to complete ride');
    }
  };

  const getStatusText = (status) => {
    const statusMap = {
      'REQUESTED': 'Waiting',
      'ACCEPTED': 'Accepted',
      'ENROUTE': 'En Route',
      'COMPLETED': 'Completed',
      'CANCELLED': 'Cancelled'
    };
    return statusMap[status] || status;
  };

  const getStatusClass = (status) => {
    return `status-badge status-${status.toLowerCase()}`;
  };

  const renderRideCard = (ride, isAvailable = false) => (
    <div key={ride.id} className="ride-item">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h3>Ride #{ride.id}</h3>
        <span className={getStatusClass(ride.status)}>
          {getStatusText(ride.status)}
        </span>
      </div>
      
      <div className="ride-details">
        <div className="ride-detail">
          <label>📍 Pickup:</label>
          <span>{ride.pickupLocation}</span>
        </div>
        <div className="ride-detail">
          <label>🎯 Destination:</label>
          <span>{ride.destination}</span>
        </div>
        <div className="ride-detail">
          <label>💰 Fare:</label>
          <span style={{ fontWeight: 'bold', color: '#2ecc71' }}>
            ${ride.fare}
          </span>
        </div>
        <div className="ride-detail">
          <label>🕐 Requested:</label>
          <span>{new Date(ride.requestedTime).toLocaleString('en-US')}</span>
        </div>
        {ride.completedTime && (
          <div className="ride-detail">
            <label>✅ Completed:</label>
            <span>{new Date(ride.completedTime).toLocaleString('en-US')}</span>
          </div>
        )}
      </div>

      {isAvailable && (
        <button 
          className="btn btn-success" 
          onClick={() => handleAcceptRide(ride.id)}
          disabled={loading}
          style={{ width: '100%', marginTop: '1rem' }}
        >
          {loading ? 'Accepting...' : '🚗 Accept Ride'}
        </button>
      )}

      {ride.status === 'ACCEPTED' && ride.driverId === user.id && (
        <button 
          className="btn btn-primary" 
          onClick={() => handleStartRide(ride.id)}
          style={{ width: '100%', marginTop: '1rem' }}
        >
          🚀 Start Trip
        </button>
      )}

      {ride.status === 'ENROUTE' && ride.driverId === user.id && (
        <button 
          className="btn btn-success" 
          onClick={() => handleCompleteRide(ride.id)}
          style={{ width: '100%', marginTop: '1rem' }}
        >
          ✅ Complete Ride
        </button>
      )}
    </div>
  );

  return (
    <div>
      <h2>Driver Dashboard</h2>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Stats Card */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <h3>📊 My Statistics</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '2rem', marginTop: '1rem' }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', color: '#2ecc71', fontWeight: 'bold' }}>
              ${user.walletBalance || 0}
            </div>
            <div style={{ color: '#7f8c8d', marginTop: '0.5rem' }}>Account Balance</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', color: '#3498db', fontWeight: 'bold' }}>
              {myRides.filter(r => r.status === 'COMPLETED').length}
            </div>
            <div style={{ color: '#7f8c8d', marginTop: '0.5rem' }}>Completed Rides</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', color: '#e67e22', fontWeight: 'bold' }}>
              {myRides.filter(r => r.status === 'ACCEPTED' || r.status === 'ENROUTE').length}
            </div>
            <div style={{ color: '#7f8c8d', marginTop: '0.5rem' }}>Active Rides</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem' }}>
        <button 
          className={`btn ${activeTab === 'available' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('available')}
        >
          Available Rides ({availableRides.length})
        </button>
        <button 
          className={`btn ${activeTab === 'history' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('history')}
        >
          My Rides ({myRides.length})
        </button>
      </div>

      {/* Content */}
      {activeTab === 'available' && (
        <div className="card">
          <h3>🚗 Available Rides</h3>
          {availableRides.length === 0 ? (
            <div style={{ textAlign: 'center', color: '#7f8c8d', padding: '2rem' }}>
              <p>No available rides</p>
              <p style={{ fontSize: '0.9rem', marginTop: '0.5rem' }}>
                Auto-refresh every 10 seconds
              </p>
            </div>
          ) : (
            <div className="ride-list">
              {availableRides.map((ride) => renderRideCard(ride, true))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'history' && (
        <div className="card">
          <h3>📋 My Rides</h3>
          {myRides.length === 0 ? (
            <p style={{ textAlign: 'center', color: '#7f8c8d', padding: '2rem' }}>
              No rides yet
            </p>
          ) : (
            <div className="ride-list">
              {myRides.map((ride) => renderRideCard(ride, false))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default DriverDashboard;
