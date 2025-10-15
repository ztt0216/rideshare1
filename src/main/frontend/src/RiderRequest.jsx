// src/RiderRequest.jsx
import { useState } from "react";
import { createRide, formatAud } from "./api";

export default function RiderRequest({ onRideCreated, onWalletChange }) {
  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");
  const [preview, setPreview] = useState(null);
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);

  function estimateFare(o, d) {
    const toInt = (value) => {
      const s = String(value ?? "").trim();
      return s === "" ? Number.NaN : parseInt(s, 10);
    };

    const a = toInt(o);
    const b = toInt(d);

    const isAirport = (x) => x === 3045;
    const isVic = (x) => x >= 3000 && x <= 3999;
    const isMetro = (x) => x >= 3000 && x <= 3299;
    const isRegional = (x) => isVic(x) && !isMetro(x);

    if (isAirport(a) || isAirport(b)) return 60.0;
    if (!isVic(a) || !isVic(b)) return 500.0;
    if (isRegional(a) || isRegional(b)) return 220.0;
    return 40.0;
  }

  function onPreview() {
    setMsg("");
    if (!/^\d{4}$/.test(origin) || !/^\d{4}$/.test(destination)) {
      setMsg("Please enter two valid 4-digit postcodes.");
      setPreview(null);
      return;
    }
    const fare = estimateFare(origin, destination);
    setPreview({ fare, origin, destination });
  }

  async function onConfirm() {
    if (!preview) return;
    setLoading(true);
    setMsg("");
    try {
      const response = await createRide({
        origin: preview.origin,
        destination: preview.destination,
      });
      const rideId = response?.rideId ?? response?.id;
      setOrigin("");
      setDestination("");
      setPreview(null);
      setMsg(
        rideId
          ? `Ride #${rideId} requested successfully.`
          : "Ride requested successfully."
      );
      onRideCreated?.();
      onWalletChange?.();
    } catch (error) {
      setMsg(error?.message || "Request failed");
    } finally {
      setLoading(false);
    }
  }

  function onCancel() {
    setPreview(null);
  }

  return (
    <div className="column gap">
      <h3>Request a Ride</h3>

      <div className="req-card">
        <div className="req-row">
          <div className="pill-field grow">
            <label className="pill-label">Pickup Postcode</label>
            <input
              className="pill-input"
              inputMode="numeric"
              maxLength={4}
              placeholder="e.g. 3000"
              value={origin}
              onChange={(e) =>
                setOrigin(e.target.value.replace(/\D/g, "").slice(0, 4))
              }
            />
          </div>

          <div className="pill-field grow">
            <label className="pill-label">Destination Postcode</label>
            <input
              className="pill-input"
              inputMode="numeric"
              maxLength={4}
              placeholder="e.g. 3045"
              value={destination}
              onChange={(e) =>
                setDestination(e.target.value.replace(/\D/g, "").slice(0, 4))
              }
            />
          </div>

          <div className="pill-field add-field">
            <label className="pill-label" aria-hidden="true" style={{ visibility: "hidden" }}>
              Action
            </label>
            <button className="req-btn" onClick={onPreview} disabled={loading}>
              Get Fare &amp; Confirm
            </button>
          </div>
        </div>
      </div>

      {msg && <p className="msg">{msg}</p>}

      {preview && (
        <div className="confirm-bar">
          <div className="confirm-info">
            <span>
              <strong>Fare:</strong> {formatAud(preview.fare)}
            </span>
            <span className="dot">•</span>
            <span>
              <strong>Route:</strong> {preview.origin} → {preview.destination}
            </span>
          </div>

            <div className="confirm-actions">
            <button
              className="req-btn"
              onClick={onConfirm}
              disabled={loading}
              aria-label="Confirm request"
            >
              {loading ? "Submitting..." : "Confirm"}
            </button>
            <button className="req-btn ghost" onClick={onCancel} disabled={loading}>
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
