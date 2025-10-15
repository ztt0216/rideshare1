// src/App.jsx
import { useEffect, useSyncExternalStore } from "react";
import Login from "./Login.jsx";
import Register from "./Register.jsx";
import Dashboard from "./Dashboard.jsx";

const subscribe = (on) => {
  const handler = () => on();
  window.addEventListener("hashchange", handler);
  return () => window.removeEventListener("hashchange", handler);
};
const getSnapshot = () => window.location.hash || "#login";
const getServerSnapshot = () => "#login";

function getSessionUser() {
  try { return JSON.parse(sessionStorage.getItem("session_user") || "null"); }
  catch { return null; }
}
function goByRole(u) {
  const role = String(u?.role || "").toUpperCase();
  const target = role === "DRIVER" ? "#driver" : "#rider";
  if (window.location.hash !== target) {
    window.location.hash = target;
  } else {
    // When same hash, manually trigger a render
    window.dispatchEvent(new HashChangeEvent("hashchange"));
  }
}

export default function App() {
  const route = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  // First time without hash → #login
  useEffect(() => { if (!window.location.hash) window.location.hash = "#login"; }, []);

  // Logged in users are not allowed to stay on login/register
  useEffect(() => {
    const u = getSessionUser();
    if (u && (route === "#login" || route === "#register" || route === "")) {
      goByRole(u);
    }
    if (!u && !(route === "#login" || route === "#register")) {
      window.location.hash = "#login";
    }
  }, [route]);

  if (route === "#register") return <Register />;
  if (route === "#login") return <Login />;
  return <Dashboard />; // #driver / #rider
}
