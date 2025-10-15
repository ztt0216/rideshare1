// src/PageShell.jsx
export default function PageShell({ title = "Rideshare", brandSize = "md", right = null, children }) {
  return (
    <div className="shell">
      <header className="brandbar">
        <h1 className={`brand ${brandSize === "sm" ? "brand-sm" : ""}`}>{title}</h1>
        {right && <div className="brand-right">{right}</div>}
      </header>
      <main className="page center">
        {children}
      </main>
    </div>
  );
}
