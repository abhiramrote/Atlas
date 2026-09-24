import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, Loader2 } from "lucide-react";

import { setToken } from "../api/auth";
import { useAuth } from "../context/AuthContext";

function AuthCallbackPage() {
  const navigate = useNavigate();
  const { refresh } = useAuth();

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const hash = window.location.hash;

    if (!hash || !hash.startsWith("#token=")) {
      setError(
        "No authentication token was found in the redirect. " +
          "The login may have been cancelled or failed upstream."
      );
      return;
    }

    const token = hash.slice("#token=".length);

    if (!token) {
      setError("The authentication token was empty.");
      return;
    }

    setToken(token);

    window.history.replaceState(null, "", window.location.pathname);

    void refresh().then(() => {
      navigate("/", { replace: true });
    });
  }, [navigate, refresh]);

  if (error) {
    return (
      <main className="dashboard-shell">
        <div className="state-card error-state" style={{ marginTop: 60 }}>
          <AlertTriangle size={26} />
          <h4>Sign in failed</h4>
          <p>{error}</p>
          <button type="button" onClick={() => navigate("/", { replace: true })}>
            Back to dashboard
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-shell">
      <div className="state-card" style={{ marginTop: 60 }}>
        <Loader2 size={28} className="spin" />
        <p>Completing sign in...</p>
      </div>
    </main>
  );
}

export default AuthCallbackPage;
