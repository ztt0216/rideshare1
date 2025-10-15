// src/RiderRides.jsx
import { useEffect, useMemo, useState } from "react";
import { cancelRide, fetchRiderHistory, formatAud } from "./api";

const ACTIVE_STATUSES = new Set(["REQUESTED", "ACCEPTED", "ENROUTE"]);

function toDisplayDate(timestamp) {
  if (!timestamp) return "";
  const date = new Date(Number(timestamp));
  if (Number.isNaN(date.getTime())) return "";
  return date
    .toLocaleString("en-AU", {
      timeZone: "Australia/Melbourne",
      hour12: false,
    })
    .replace("T", " ");
}

function sortRides(rides = []) {
  return [...rides].sort((a, b) => (b.rideId ?? 0) - (a.rideId ?? 0));
}

export default function RiderRides({ refreshSignal = 0, onWalletChange }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [rides, setRides] = useState([]);

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const data = await fetchRiderHistory();
        if (mounted) {
          setRides(sortRides(data));
        }
      } catch (e) {
        if (mounted) setError(e?.message || "Failed to load rides.");
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [refreshSignal]);

  const activeRides = useMemo(
    () => rides.filter((ride) => ACTIVE_STATUSES.has((ride.status || "").toUpperCase())),
    [rides]
  );
  const historyRides = useMemo(
    () => rides.filter((ride) => !ACTIVE_STATUSES.has((ride.status || "").toUpperCase())),
    [rides]
  );

  async function handleCancel(rideId) {
    if (!rideId) return;
    setError("");
    try {
      await cancelRide(rideId);
      const data = await fetchRiderHistory();
      setRides(sortRides(data));
      onWalletChange?.();
    } catch (e) {
      setError(e?.message || "Cancel ride failed.");
    }
  }

  return (
    <div className="column gap">
      <h3>My Rides</h3>
      {loading && rides.length === 0 ? <p>Loading…</p> : null}
      {error && <p className="msg">{error}</p>}

      {!loading && activeRides.length === 0 && historyRides.length === 0 ? (
        <p>You have no rides yet.</p>
      ) : null}

      {activeRides.length > 0 && (
        <section className="column gap">
          <h4>Active</h4>
          {activeRides.map((ride) => (
            <div key={ride.rideId} className="ride-card">
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
                    <span>Driver: {ride.counterparty}</span>
                  </>
                ) : null}
              </div>
              {(ride.status === "REQUESTED" || ride.status === "ACCEPTED") && (
                <button className="ride-accept" onClick={() => handleCancel(ride.rideId)}>
                  Cancel Ride
                </button>
              )}
            </div>
          ))}
        </section>
      )}

      {historyRides.length > 0 && (
        <section className="column gap">
          <h4>History</h4>
          {historyRides.map((ride) => (
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
                    <span>Driver: {ride.counterparty}</span>
                  </>
                ) : null}
                {ride.paymentTimestamp ? (
                  <>
                    <span className="dot">•</span>
                    <span>{toDisplayDate(ride.paymentTimestamp)}</span>
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
