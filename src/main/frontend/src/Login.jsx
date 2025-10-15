// src/Login.jsx
import { useState } from "react";
import { authLogin } from "./api";

export default function Login() {
  // Only pre-fill last_email when switch remember_email==="1"
  const remembered = localStorage.getItem("remember_email") === "1";
  const [email, setEmail] = useState(remembered ? (localStorage.getItem("last_email") || "") : "");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(remembered); // Initial checkbox state syncs with whether to pre-fill
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);

   async function onSubmit(e) {
    e.preventDefault();
    setMsg(""); setLoading(true);
    try {
      const u = await authLogin({ email, password });

      // Based on whether it's checked this time, write or clear local memory
      if (remember) {
        localStorage.setItem("remember_email", "1");
        localStorage.setItem("last_email", email);
      } else {
        localStorage.removeItem("remember_email");
        localStorage.removeItem("last_email");
      }

      const r = String(u.role || "").toUpperCase();
      window.location.hash = r === "DRIVER" ? "#driver" : "#rider";
      setTimeout(() => window.dispatchEvent(new HashChangeEvent("hashchange")), 0);
    } catch (err) {
      setMsg(err?.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth">
      {/* Top brand title (artistic font) */}
      <h1 className="brand">Rideshare</h1>

      {/* Two columns: left form / right hero */}
      <div className="page two-col auth-grid">
        <div className="card column gap" style={{ flex: "0 0 460px" }}>
          <h2>Sign in</h2>

          <form onSubmit={onSubmit} className="column gap form-styled">
            <label>
              <div>Email</div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
              />
            </label>

            <label>
              <div>Password</div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Your password"
                required
              />
            </label>

            {/* Remember me: same line */}
            <div className="inline">
              <input
                id="remember"
                type="checkbox"
                checked={remember}
                onChange={(e) => setRemember(e.target.checked)}
              />
              <label htmlFor="remember">Remember me</label>
            </div>

            <button type="submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign in"}
            </button>

            {msg && <p className="msg">{msg}</p>}
          </form>

          <p style={{ marginTop: 16 }}>
            No account yet? <a href="#register">Create account</a>
          </p>
        </div>

        {/* Right Hero: absolute positioning to fill, preventing bottom green bar */}
        <div className="hero">
          <img
            className="hero-img"
            src="/images/ride-hero.jpg"
            alt="Rideshare illustration"
            loading="eager"
            draggable="false"
          />
        </div>
      </div>
    </div>
  );
}
