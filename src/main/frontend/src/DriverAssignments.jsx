// src/DriverAssignments.jsx
import { useEffect, useMemo, useState } from "react";
import {
  beginRide,
  completeRide,
  fetchDriverHistory,
  formatAud,
} from "./api";

const STARTABLE = "ACCEPTED";
const COMPLETEABLE = "ENROUTE";

function statusKey(status = "") {
  return status.toUpperCase();
}

export default function DriverAssignments({ refreshSignal = 0, onWalletChange }) {
  const [rides, setRides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setMessage("");
      try {
        const data = await fetchDriverHistory();
        if (mounted) setRides([...data]);
      } catch (e) {
        if (mounted) setMessage(e?.message || "Failed to load my rides.");
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [refreshSignal]);

  const grouped = useMemo(() => {
    const buckets = { ACTIVE: [], HISTORY: [] };
    rides.forEach((ride) => {
      const status = statusKey(ride.status);
      if (status === STARTABLE || status === COMPLETEABLE) {
        buckets.ACTIVE.push(ride);
      } else if (status === "ACCEPTED" || status === "ENROUTE") {
        buckets.ACTIVE.push(ride);
      } else {
        buckets.HISTORY.push(ride);
      }
    });
    buckets.ACTIVE.sort((a, b) => (b.rideId ?? 0) - (a.rideId ?? 0));
    buckets.HISTORY.sort((a, b) => (b.rideId ?? 0) - (a.rideId ?? 0));
    return buckets;
  }, [rides]);

  async function act(rideId, action) {
    if (!rideId) return;
    try {
      if (action === "begin") await beginRide(rideId);
      else if (action === "complete") await completeRide(rideId);
      const data = await fetchDriverHistory();
      setRides([...data]);
      if (action === "complete") onWalletChange?.();
      setMessage("");
    } catch (e) {
      setMessage(e?.message || `Failed to ${action} ride.`);
    }
  }

  return (
    <div className="column gap">
      <h3>Assigned Rides</h3>
      {loading && rides.length === 0 ? <p>Loading…</p> : null}
      {message && <p className="msg">{message}</p>}

      {grouped.ACTIVE.length === 0 && grouped.HISTORY.length === 0 && !loading ? (
        <p>No rides assigned yet.</p>
      ) : null}

      {grouped.ACTIVE.length > 0 && (
        <section className="column gap">
          <h4>In Progress</h4>
          {grouped.ACTIVE.map((ride) => {
            const status = statusKey(ride.status);
            const canBegin = status === STARTABLE;
            const canComplete = status === COMPLETEABLE;
            return (
              <div key={`active-${ride.rideId}`} className="ride-card">
                <div className="ride-head">
                  <span className="ride-id">#{ride.rideId}</span>
                  <span className="ride-route">
                    — {ride.pickup} → {ride.destination}
                  </span>
                </div>
                <div className="ride-meta">
                  <span className="ride-status">{ride.status}</span>
                  {ride.fare ? (
                    <>
                      <span className="dot">•</span>
                      <span>Fare: {formatAud(ride.fare)}</span>
                    </>
                  ) : null}
                  {ride.counterparty ? (
                    <>
                      <span className="dot">•</span>
                      <span>Rider: {ride.counterparty}</span>
                    </>
                  ) : null}
                </div>
                <div className="row gap">
                  <button
                    className="ride-accept"
                    disabled={!canBegin}
                    onClick={() => act(ride.rideId, "begin")}
                  >
                    Begin Ride
                  </button>
                  <button
                    className="ride-accept"
                    disabled={!canComplete}
                    onClick={() => act(ride.rideId, "complete")}
                  >
                    Complete Ride
                  </button>
                </div>
              </div>
            );
          })}
        </section>
      )}

      {grouped.HISTORY.length > 0 && (
        <section className="column gap">
          <h4>History</h4>
          {grouped.HISTORY.map((ride) => (
            <div key={`history-${ride.rideId}`} className="ride-card muted">
              <div className="ride-head">
                <span className="ride-id">#{ride.rideId}</span>
                <span className="ride-route">
                  — {ride.pickup} → {ride.destination}
                </span>
              </div>
              <div className="ride-meta">
                <span className="ride-status">{ride.status}</span>
                {ride.fare ? (
                  <>
                    <span className="dot">•</span>
                    <span>Fare: {formatAud(ride.fare)}</span>
                  </>
                ) : null}
                {ride.counterparty ? (
                  <>
                    <span className="dot">•</span>
                    <span>Rider: {ride.counterparty}</span>
                  </>
                ) : null}
              </div>
            </div>
          ))}
        </section>
      )}
    </div>
  );
}
