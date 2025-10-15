// src/Dashboard.jsx
import { useEffect, useState } from "react";
import PageShell from "./PageShell";
import { authLogout, me } from "./api";
import RiderRequest from "./RiderRequest.jsx";
import RiderRides from "./RiderRides.jsx";
import DriverRides from "./DriverRides.jsx";
import DriverAssignments from "./DriverAssignments.jsx";
import Availability from "./Availability.jsx";
import WalletPanel from "./WalletPanel.jsx";

export default function Dashboard() {
  const [user] = useState(me());
  const role = String(user?.role || "").toUpperCase();

  const [driverTab, setDriverTab] = useState("available");
  const [walletRefreshKey, setWalletRefreshKey] = useState(0);
  const [riderRefreshKey, setRiderRefreshKey] = useState(0);
  const [driverAssignedKey, setDriverAssignedKey] = useState(0);
  const [driverAvailableKey, setDriverAvailableKey] = useState(0);

  useEffect(() => {
    const applyHash = () => {
      const h = (window.location.hash || "").toLowerCase();
      if (h.includes("driver")) setDriverTab("available");
    };
    applyHash();
    window.addEventListener("hashchange", applyHash);
    return () => window.removeEventListener("hashchange", applyHash);
  }, []);

  const triggerWalletRefresh = () => setWalletRefreshKey((v) => v + 1);
  const triggerRiderRefresh = () => setRiderRefreshKey((v) => v + 1);
  const triggerDriverAssignedRefresh = () => setDriverAssignedKey((v) => v + 1);
  const triggerDriverAvailableRefresh = () => setDriverAvailableKey((v) => v + 1);

  const rightActions = (
    <div className="row gap">
      <span className="pill">{role || "USER"}</span>
      <button
        className="ghost"
        onClick={async () => {
          try {
            await authLogout();
          } finally {
            window.location.hash = "#login";
            setTimeout(
              () => window.dispatchEvent(new HashChangeEvent("hashchange")),
              0
            );
          }
        }}
      >
        Logout
      </button>
    </div>
  );

  return (
    <PageShell title="Rideshare" brandSize="sm" right={rightActions}>
      <div className="center-stack">
        <h2 className="welcome">Welcome, {user?.name || "Guest"}</h2>

        <WalletPanel
          refreshSignal={walletRefreshKey}
          onTopUp={triggerWalletRefresh}
        />

        {role === "DRIVER" ? (
          <>
            <div className="tabs">
              <button
                className={`tab ${driverTab === "available" ? "active" : ""}`}
                onClick={() => setDriverTab("available")}
              >
                Available
              </button>
              <button
                className={`tab ${driverTab === "assigned" ? "active" : ""}`}
                onClick={() => setDriverTab("assigned")}
              >
                Assigned
              </button>
              <button
                className={`tab ${driverTab === "availability" ? "active" : ""}`}
                onClick={() => setDriverTab("availability")}
              >
                Availability
              </button>
            </div>

            {driverTab === "available" && (
              <DriverRides
                refreshSignal={driverAvailableKey}
                onActionComplete={() => {
                  triggerDriverAssignedRefresh();
                  triggerDriverAvailableRefresh();
                }}
              />
            )}

            {driverTab === "assigned" && (
              <DriverAssignments
                refreshSignal={driverAssignedKey}
                onWalletChange={triggerWalletRefresh}
              />
            )}

            {driverTab === "availability" && <Availability />}
          </>
        ) : (
          <>
            <RiderRequest
              onRideCreated={() => {
                triggerRiderRefresh();
                triggerWalletRefresh();
              }}
              onWalletChange={triggerWalletRefresh}
            />
            <RiderRides
              refreshSignal={riderRefreshKey}
              onWalletChange={triggerWalletRefresh}
            />
          </>
        )}
      </div>
    </PageShell>
  );
}
