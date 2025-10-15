import React, { useState, useEffect } from 'react';
import { requestRide, getRiderHistory, updateWallet, getUser, cancelRide } from '../services/api';

function RiderDashboard({ user, onUpdateUser }) {
  const [rides, setRides] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showRequestForm, setShowRequestForm] = useState(false);
  const [walletAmount, setWalletAmount] = useState('');
  
  const [rideForm, setRideForm] = useState({
    pickupLocation: '',
    destination: ''
  });

  useEffect(() => {
    loadRides();
  }, []);

  const loadRides = async () => {
    try {
      const history = await getRiderHistory(user.id);
      setRides(history);
    } catch (err) {
      console.error('Failed to load rides:', err);
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

  const handleRequestRide = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const response = await requestRide({
        riderId: user.id,
        ...rideForm
      });
      setSuccess(`Ride created successfully! Ride ID: ${response.rideId}`);
      setRideForm({ pickupLocation: '', destination: '' });
      setShowRequestForm(false);
      loadRides();
      refreshUserData();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create ride');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelRide = async (rideId) => {
    if (!window.confirm('Are you sure you want to cancel this ride?')) return;
    
    try {
      await cancelRide(rideId, user.id);
      setSuccess('Ride cancelled successfully');
      loadRides();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to cancel ride');
    }
  };

  const handleAddMoney = async (e) => {
    e.preventDefault();
    try {
      await updateWallet(user.id, parseFloat(walletAmount));
      setSuccess(`Successfully added $${walletAmount}`);
      setWalletAmount('');
      refreshUserData();
    } catch (err) {
      setError('Failed to add money');
    }
  };

  const getStatusText = (status) => {
    const statusMap = {
      'REQUESTED': 'Waiting for Driver',
      'ACCEPTED': 'Driver Accepted',
      'ENROUTE': 'En Route',
      'COMPLETED': 'Completed',
      'CANCELLED': 'Cancelled'
    };
    return statusMap[status] || status;
  };

  const getStatusClass = (status) => {
    return `status-badge status-${status.toLowerCase()}`;
  };

  return (
    <div>
      <h2>Rider Dashboard</h2>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="grid-2" style={{ marginBottom: '2rem' }}>
        {/* Wallet Card */}
        <div className="card">
          <h3>My Wallet</h3>
          <div style={{ fontSize: '2rem', color: '#2ecc71', margin: '1rem 0' }}>
            ${user.walletBalance || 0}
          </div>
          <form onSubmit={handleAddMoney}>
            <div className="form-group">
              <input
                type="number"
                step="0.01"
                min="0"
                value={walletAmount}
                onChange={(e) => setWalletAmount(e.target.value)}
                placeholder="Enter amount to add"
              />
            </div>
            <button type="submit" className="btn btn-success" style={{ width: '100%' }}>
              💰 Add Money
            </button>
          </form>
        </div>

        {/* Request Ride Card */}
        <div className="card">
          <h3>Ride Service</h3>
          <p style={{ color: '#7f8c8d', marginBottom: '1rem' }}>
            Fare Information:
            <br />• City (3000-3299): $40
            <br />• Airport (3045): $60
            <br />• Regional (3300-3999): $220
            <br />• Interstate (Other): $500
          </p>
          <button 
            className="btn btn-primary" 
            onClick={() => setShowRequestForm(!showRequestForm)}
            style={{ width: '100%' }}
          >
            🚗 {showRequestForm ? 'Cancel' : 'Request Ride'}
          </button>
        </div>
      </div>

      {/* Request Ride Form */}
      {showRequestForm && (
        <div className="card">
          <h3>Create Ride</h3>
          <form onSubmit={handleRequestRide}>
            <div className="form-group">
              <label>Pickup Location</label>
              <input
                type="text"
                value={rideForm.pickupLocation}
                onChange={(e) => setRideForm({ ...rideForm, pickupLocation: e.target.value })}
                placeholder="e.g., 123 Collins St, Melbourne VIC 3000"
                required
              />
            </div>

            <div className="form-group">
              <label>Destination (must include postcode)</label>
              <input
                type="text"
                value={rideForm.destination}
                onChange={(e) => setRideForm({ ...rideForm, destination: e.target.value })}
                placeholder="e.g., Melbourne Airport, VIC 3045"
                required
              />
            </div>

            <button 
              type="submit" 
              className="btn btn-primary" 
              disabled={loading}
              style={{ width: '100%' }}
            >
              {loading ? 'Creating...' : 'Confirm Request'}
            </button>
          </form>
        </div>
      )}

      {/* Ride History */}
      <div className="card">
        <h3>My Rides</h3>
        {rides.length === 0 ? (
          <p style={{ textAlign: 'center', color: '#7f8c8d', padding: '2rem' }}>
            No rides yet
          </p>
        ) : (
          <div className="ride-list">
            {rides.map((ride) => (
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
                    <label>💵 Fare:</label>
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

                {ride.status === 'REQUESTED' && (
                  <button 
                    className="btn btn-danger" 
                    onClick={() => handleCancelRide(ride.id)}
                    style={{ width: '100%', marginTop: '1rem' }}
                  >
                    Cancel Ride
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default RiderDashboard;
