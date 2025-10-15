// src/Availability.jsx
import { useEffect, useState } from "react";
import { addAvailability, deleteAvailability, listAvailability } from "./api";

const DAYS = [
  { value: "MON", label: "Monday" },
  { value: "TUE", label: "Tuesday" },
  { value: "WED", label: "Wednesday" },
  { value: "THU", label: "Thursday" },
  { value: "FRI", label: "Friday" },
  { value: "SAT", label: "Saturday" },
  { value: "SUN", label: "Sunday" },
];

export default function Availability() {
  const [day, setDay] = useState(DAYS[new Date().getDay()]?.value ?? "MON");
  const [start, setStart] = useState("09:00");
  const [end, setEnd] = useState("17:00");
  const [slots, setSlots] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setMessage("");
    try {
      const data = await listAvailability();
      setSlots(
        (data || []).map((slot, index) => ({
          id: slot.id ?? index,
          day: slot.day ?? slot.availableDay ?? "",
          start: slot.start ?? slot.startTime ?? "",
          end: slot.end ?? slot.endTime ?? "",
        }))
      );
    } catch (e) {
      setMessage(e?.message || "Failed to load availability.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function onAdd() {
    setMessage("");
    if (!start || !end || end <= start) {
      setMessage("End time must be after start time.");
      return;
    }
    try {
      await addAvailability({ availableDay: day, startTime: start, endTime: end });
      setMessage("Slot added.");
      await load();
    } catch (e) {
      setMessage(e?.message || "Failed to add availability slot.");
    }
  }

  async function onRemove(slot) {
    setMessage("");
    try {
      await deleteAvailability(slot);
      await load();
    } catch (e) {
      setMessage(e?.message || "Removing slots is not supported yet.");
    }
  }

  return (
    <div className="avail-wrap">
      <h3 className="section-title">My Weekly Availability</h3>

      <div className="avail-box">
        <div className="avail-row">
          <div className="pill-field">
            <div className="pill-label">Day</div>
            <select
              className="pill-input"
              value={day}
              onChange={(e) => setDay(e.target.value)}
            >
              {DAYS.map((d) => (
                <option key={d.value} value={d.value}>
                  {d.label}
                </option>
              ))}
            </select>
          </div>

          <div className="pill-field">
            <div className="pill-label">Start</div>
            <input
              className="pill-input"
              type="time"
              value={start}
              onChange={(e) => setStart(e.target.value)}
            />
          </div>

          <div className="pill-field">
            <div className="pill-label">End</div>
            <input
              className="pill-input"
              type="time"
              value={end}
              onChange={(e) => setEnd(e.target.value)}
            />
          </div>

          <div className="pill-field add-field">
            <div className="pill-label">&nbsp;</div>
            <button className="add-btn" onClick={onAdd} disabled={loading}>
              {loading ? "Adding…" : "Add Slot"}
            </button>
          </div>
        </div>
      </div>

      {loading && slots.length === 0 ? <p>Loading…</p> : null}

      <div className="slot-list">
        {slots.length === 0 && !loading ? (
          <p className="muted">No availability yet — add your first slot above.</p>
        ) : null}

        {slots.map((slot) => (
          <div key={`${slot.id}-${slot.day}-${slot.start}`} className="slot-item">
            <div className="slot-meta">
              <span className="slot-day">{slot.day}</span>
              <span className="slot-time">
                {slot.start?.slice(0, 5)}–{slot.end?.slice(0, 5)}
              </span>
            </div>
            <button className="slot-remove" onClick={() => onRemove(slot)} disabled={loading}>
              Remove
            </button>
          </div>
        ))}
      </div>

      {message && <p className="msg" style={{ marginTop: 12 }}>{message}</p>}
    </div>
  );
}
