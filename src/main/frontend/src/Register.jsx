// src/Register.jsx
import { useState } from "react";
import PageShell from "./PageShell";
import { authRegister } from "./api";

export default function Register() {
  const MIN_PW = 6; // Minimum length aligned between frontend and backend

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("Rider");
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setMsg("");

    // Minimum length validation before submission (consistent with backend)
    if ((password || "").length < MIN_PW) {
      setMsg(`Password must be at least ${MIN_PW} characters`);
      return;
    }

    setLoading(true);
    try {
      const u = await authRegister({
        name: name.trim(),
        email: email.trim(),
        password,
        role: role.toUpperCase(),
      });

      // Remember email (for next login convenience)
      localStorage.setItem("last_email", email.trim());

      // Enter corresponding dashboard after successful registration
      const r = String(u.role || role).toUpperCase();
      window.location.hash = r === "DRIVER" ? "#driver" : "#rider";
      setTimeout(() => window.dispatchEvent(new HashChangeEvent("hashchange")), 0);
    } catch (err) {
      setMsg(err?.message || "Register failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageShell title="Rideshare" brandSize="sm">
      <div className="center-stack">
        <div className="card column gap" style={{ maxWidth: 560, margin: "0 auto" }}>
          <h2>Create account</h2>

          <form onSubmit={onSubmit} className="column gap form-styled" noValidate>
            <label>
              <div>Name</div>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Tracy Teacher"
                required
                autoComplete="name"
              />
            </label>

            <label>
              <div>Email</div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                autoComplete="email"
              />
            </label>

            <label>
              <div>Password</div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={`Minimum ${MIN_PW} chars`}
                minLength={MIN_PW}
                required
                autoComplete="new-password"
              />
            </label>

            <label>
              <div>Role</div>
              <select value={role} onChange={(e) => setRole(e.target.value)}>
                <option>Rider</option>
                <option>Driver</option>
              </select>
            </label>

            <button type="submit" disabled={loading}>
              {loading ? "Registering..." : "Register"}
            </button>

            {msg && <p className="msg">{msg}</p>}
          </form>

          <p style={{ marginTop: 16 }}>
            Already have an account? <a href="#login">Sign in</a>
          </p>
        </div>
      </div>
    </PageShell>
  );
}
