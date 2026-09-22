import { Link } from "react-router-dom";
import { FileText } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Activity,
  BarChart3,
  Database,
  RefreshCw,
  ShieldAlert,
  Sparkles,
  TrendingUp,
} from "lucide-react";

import { getOpportunities } from "../api/atlasApi";
import OpportunityCard from "../components/OpportunityCard";
import type { Opportunity } from "../types/atlas";

function DashboardPage() {
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = useCallback(async (showRefreshState = false) => {
    try {
      if (showRefreshState) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);

      const data = await getOpportunities();
      setOpportunities(data);
    } catch (requestError) {
      const message =
        requestError instanceof Error
          ? requestError.message
          : "Unable to load Atlas opportunities";

      setError(message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  const highestScore = useMemo(() => {
    if (opportunities.length === 0) {
      return 0;
    }

    return Math.max(...opportunities.map((item) => item.score));
  }, [opportunities]);

  const strongCount = useMemo(() => {
    return opportunities.filter((item) => {
      const rating = item.rating.toUpperCase();
      return rating === "STRONG" || rating === "ELITE";
    }).length;
  }, [opportunities]);

  const scoringPolicy =
    opportunities.length > 0 ? opportunities[0].policyVersion : "Not available";

  return (
    <main className="dashboard-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark">
            <TrendingUp size={24} />
          </div>

          <div>
            <h1>Atlas</h1>
            <p>Opportunity Intelligence Platform</p>
          </div>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
  <Link className="refresh-button" to="/theses">
    <FileText size={16} />
    Research journal
  </Link>

  <button
    className="refresh-button"
    type="button"
    disabled={refreshing}
    onClick={() => void loadDashboard(true)}
  >
    <RefreshCw size={17} className={refreshing ? "spin" : ""} />
    {refreshing ? "Refreshing" : "Refresh dashboard"}
  </button>
</div>

        <button
          className="refresh-button"
          type="button"
          disabled={refreshing}
          onClick={() => void loadDashboard(true)}
        >
          <RefreshCw size={17} className={refreshing ? "spin" : ""} />
          {refreshing ? "Refreshing" : "Refresh dashboard"}
        </button>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <div className="eyebrow">
            <Sparkles size={15} />
            Market intelligence dashboard
          </div>

          <h2>
            Discover opportunities with
            <span> transparent scoring.</span>
          </h2>

          <p>
            Atlas combines financial growth, profitability, operating cash flow,
            and price momentum into inspectable opportunity rankings.
          </p>
        </div>

        <div className="live-card">
          <div className="live-indicator">
            <span />
            Atlas API connected
          </div>

          <p>Scoring policy</p>
          <strong>{scoringPolicy}</strong>
        </div>
      </section>

      <section className="metric-grid">
        <article className="metric-card">
          <div className="metric-icon purple">
            <Database size={20} />
          </div>

          <div>
            <span>Tracked opportunities</span>
            <strong>{opportunities.length}</strong>
          </div>
        </article>

        <article className="metric-card">
          <div className="metric-icon green">
            <BarChart3 size={20} />
          </div>

          <div>
            <span>Highest score</span>
            <strong>{highestScore}</strong>
          </div>
        </article>

        <article className="metric-card">
          <div className="metric-icon blue">
            <Activity size={20} />
          </div>

          <div>
            <span>Strong opportunities</span>
            <strong>{strongCount}</strong>
          </div>
        </article>
      </section>

      <section className="development-notice">
        <ShieldAlert size={19} />
        <p>
          Atlas is in development. Live provider data currently powers selected
          price signals, while some fundamental records still use development
          fixtures. Rankings are research outputs, not investment
          recommendations.
        </p>
      </section>

      <section className="opportunity-section">
        <div className="section-heading">
          <div>
            <p className="section-kicker">Discovery engine</p>
            <h3>Top opportunities</h3>
          </div>

          <span>
            {opportunities.length}{" "}
            {opportunities.length === 1 ? "company" : "companies"}
          </span>
        </div>

        {loading && (
          <div className="state-card">
            <div className="loader" />
            <p>Loading Atlas intelligence...</p>
          </div>
        )}

        {!loading && error && (
          <div className="state-card error-state">
            <ShieldAlert size={26} />
            <h4>Dashboard unavailable</h4>
            <p>{error}</p>

            <button type="button" onClick={() => void loadDashboard()}>
              Try again
            </button>
          </div>
        )}

        {!loading && !error && opportunities.length === 0 && (
          <div className="state-card">
            <Database size={25} />
            <p>No opportunities are currently available.</p>
          </div>
        )}

        {!loading && !error && opportunities.length > 0 && (
          <div className="opportunity-grid">
            {opportunities.map((opportunity, index) => (
              <OpportunityCard
                key={opportunity.companyId}
                opportunity={opportunity}
                rank={index + 1}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default DashboardPage;
