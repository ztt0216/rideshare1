// src/DriverRides.jsx
import { useEffect, useState } from "react";
import {
  listDriverAvailableRides,
  acceptRide,
  listAvailability,
  isInsideAvailabilityNow,
  formatAud,
} from "./api";

function normalizeToIso(raw) {
  if (!raw) return null;
  if (raw instanceof Date) return raw.toISOString();
  if (typeof raw === "number") return new Date(raw).toISOString();
  let s = String(raw).trim();
  s = s.replace(" ", "T");
  const parts = s.split(".");
  if (parts.length > 1) {
    const ms = parts[1].slice(0, 3);
    s = parts[0] + (ms ? `.${ms}` : "");
  }
  return s;
}

function fmtAESTMinute(raw) {
  const iso = normalizeToIso(raw);
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return String(raw).replace("T", " ").slice(0, 16);
  }
  return d
    .toLocaleString("sv-SE", {
      timeZone: "Australia/Melbourne",
      hour12: false,
    })
    .slice(0, 16);
}

export default function DriverRides({ refreshSignal = 0, onActionComplete }) {
  const [rides, setRides] = useState([]);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [insideFromServer, setInsideFromServer] = useState(null);

  async function loadData() {
    setLoading(true);
    setMessage("");
    try {
      const [available, availabilitySlots] = await Promise.all([
        listDriverAvailableRides().catch(() => []),
        listAvailability().catch(() => []),
      ]);
      const items = Array.isArray(available?.items)
        ? available.items
        : Array.isArray(available)
        ? available
        : [];
      const inside = typeof available?.insideWindow === "boolean" ? available.insideWindow : null;
      setInsideFromServer(inside);
      if (inside === false && available?.message) {
        setMessage(available.message);
      }
      setRides(
        items.map((r, index) => ({
          id: r.id ?? r.rideId ?? index,
          pickup: r.pickup ?? r.pickup_postcode ?? r.origin ?? "",
          destination: r.destination ?? r.destination_postcode ?? r.dest ?? "",
          fare: Number(r.fare ?? r.fare_estimate ?? 0),
          requestedAt: r.requestedAt ?? r.requested_time ?? r.created_at ?? null,
        }))
      );
      setSlots(availabilitySlots);
    } catch (e) {
      setMessage(e?.message || "Failed to load rides.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [refreshSignal]);

  async function handleAccept(ride) {
    if (!ride?.id) {
      setMessage("Ride id missing from backend response; cannot accept.");
      return;
    }
    setMessage("");
    try {
      await acceptRide(ride.id);
      onActionComplete?.();
      await loadData();
    } catch (e) {
      setMessage(e?.message || "Failed to accept ride.");
    }
  }

  const insideLocal = isInsideAvailabilityNow(slots);
  const inside = insideFromServer === null ? insideLocal : insideFromServer;

  return (
    <div className="column gap">
      <h3>Requested Rides</h3>
      {message && <p className="msg">{message}</p>}
      <div className="row gap" style={{ alignItems: "center" }}>
        <button className="ghost" onClick={loadData} disabled={loading}>
          {loading ? "Refreshing…" : "Refresh"}
        </button>
        {!inside && (
          <span className="pill warning">
            Outside your availability window — update your schedule to accept rides.
          </span>
        )}
      </div>

      {loading && rides.length === 0 ? <p>Loading…</p> : null}

      {!loading && rides.length === 0 ? (
        <p>No ride requests matched your availability right now.</p>
      ) : null}

      {inside &&
        rides.map((ride) => (
          <div key={ride.id} className="ride-card">
            <div className="ride-head">
              <span className="ride-route">
                {ride.pickup} → {ride.destination}
              </span>
            </div>
            <div className="ride-meta">
              <span>
                {ride.requestedAt ? fmtAESTMinute(ride.requestedAt) : "Unknown time"}
              </span>
              <span className="dot">•</span>
              <span>Fare: {formatAud(ride.fare)}</span>
            </div>
            <button
              className="ride-accept"
              disabled={!ride.id || loading}
              onClick={() => handleAccept(ride)}
            >
              Accept
            </button>
            {!ride.id && (
              <p className="msg" style={{ marginTop: 6 }}>
                The backend did not include a ride identifier, so this entry cannot be accepted yet.
              </p>
            )}
          </div>
        ))}
    </div>
  );
}
