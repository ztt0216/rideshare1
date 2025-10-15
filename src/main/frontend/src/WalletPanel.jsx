// src/WalletPanel.jsx
import { useEffect, useState } from "react";
import { fetchWallet, formatAud, topUpWallet } from "./api";

export default function WalletPanel({ refreshSignal = 0, onTopUp }) {
  const [balance, setBalance] = useState(null);
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(true);
  const [topUpLoading, setTopUpLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setMessage("");
      try {
        const data = await fetchWallet();
        if (mounted) setBalance(Number(data?.balance ?? 0));
      } catch (e) {
        if (mounted) setMessage(e?.message || "Failed to load wallet.");
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [refreshSignal]);

  async function submitTopUp(e) {
    e.preventDefault();
    const numeric = Number(amount);
    if (!Number.isFinite(numeric) || numeric <= 0) {
      setMessage("Enter a positive amount in AUD.");
      return;
    }
    setTopUpLoading(true);
    setMessage("");
    try {
      await topUpWallet(numeric);
      const data = await fetchWallet();
      setBalance(Number(data?.balance ?? 0));
      setAmount("");
      setMessage("Top-up successful.");
      onTopUp?.();
    } catch (e) {
      setMessage(e?.message || "Top-up failed.");
    } finally {
      setTopUpLoading(false);
    }
  }

  return (
    <div className="card column gap" style={{ width: "100%" }}>
      <h3>Wallet</h3>
      {loading ? (
        <p>Loading balance…</p>
      ) : (
        <p className="balance">Current balance: {formatAud(balance ?? 0)}</p>
      )}
      {message && <p className="msg">{message}</p>}

      <form className="wallet-form row gap" onSubmit={submitTopUp}>
        <input
          type="number"
          min="1"
          step="1"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Top-up amount (AUD)"
        />
        <button type="submit" disabled={topUpLoading}>
          {topUpLoading ? "Processing…" : "Top up"}
        </button>
      </form>
    </div>
  );
}
