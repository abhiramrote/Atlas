import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Globe,
  Info,
  ShieldAlert,
  TrendingDown,
  TrendingUp,
} from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import {
  getCombinedScore,
  getCompany,
  getMomentum,
  getPrices,
} from "../api/atlasApi";

import type {
  CombinedScore,
  Company,
  Momentum,
  PriceBar,
} from "../types/atlas";

function CompanyPage() {
  const { companyId } = useParams<{ companyId: string }>();

  const [company, setCompany] = useState<Company | null>(null);
  const [score, setScore] = useState<CombinedScore | null>(null);
  const [momentum, setMomentum] = useState<Momentum | null>(null);
  const [prices, setPrices] = useState<PriceBar[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [priceNotice, setPriceNotice] = useState<string | null>(null);

  const loadCompany = useCallback(async () => {
    if (!companyId) {
      setError("No company was selected");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setPriceNotice(null);

      const companyData = await getCompany(companyId);
      setCompany(companyData);

      const [scoreResult, momentumResult, priceResult] =
        await Promise.allSettled([
          getCombinedScore(companyId),
          getMomentum(companyData.instrumentId),
          getPrices(companyData.instrumentId),
        ]);

      setScore(
        scoreResult.status === "fulfilled" ? scoreResult.value : null
      );

      setMomentum(
        momentumResult.status === "fulfilled"
          ? momentumResult.value
          : null
      );

      if (priceResult.status === "fulfilled") {
        setPrices(priceResult.value);

        if (priceResult.value.length === 0) {
          setPriceNotice(
            "No price history is stored for this instrument yet. Run a price refresh to populate market data."
          );
        }
      } else {
        setPrices([]);
        setPriceNotice(
          "Price history could not be loaded for this instrument."
        );
      }
    } catch (requestError) {
      const message =
        requestError instanceof Error
          ? requestError.message
          : "Unable to load company intelligence";

      setError(message);
    } finally {
      setLoading(false);
    }
  }, [companyId]);

  useEffect(() => {
    void loadCompany();
  }, [loadCompany]);

  const chartData = useMemo(() => {
    return prices
      .filter((bar) => bar.closePrice !== null)
      .map((bar) => ({
        date: bar.tradeDate,
        close: Number(bar.closePrice),
      }))
      .sort((a, b) => a.date.localeCompare(b.date));
  }, [prices]);

  const isUptrend =
    momentum !== null && momentum.priceChangePercent > 0;

  if (loading) {
    return (
      <main className="dashboard-shell">
        <div className="state-card" style={{ marginTop: 60 }}>
          <div className="loader" />
          <p>Loading company intelligence...</p>
        </div>
      </main>
    );
  }

  if (error || !company) {
    return (
      <main className="dashboard-shell">
        <div className="state-card error-state" style={{ marginTop: 60 }}>
          <ShieldAlert size={26} />
          <h4>Company unavailable</h4>
          <p>{error ?? "Company could not be loaded"}</p>

          <Link className="details-button" to="/">
            Back to dashboard
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-shell">
      <header className="topbar">
        <Link className="back-link" to="/">
          <ArrowLeft size={17} />
          Back to dashboard
        </Link>

        {company.website && (
          <a
            className="refresh-button"
            href={company.website}
            target="_blank"
            rel="noreferrer"
          >
            <Globe size={16} />
            Company website
          </a>
        )}
      </header>

      <section className="company-hero">
        <div className="symbol-avatar large">
          {company.symbol.slice(0, 2)}
        </div>

        <div>
          <h2>{company.symbol}</h2>
          <p>{company.companyName}</p>

          <div className="chip-row">
            <span className="chip">{company.exchange}</span>

            {company.sector && (
              <span className="chip">{company.sector}</span>
            )}

            {company.industry && (
              <span className="chip">{company.industry}</span>
            )}
          </div>
        </div>
      </section>

      <section className="metric-grid">
        <article className="metric-card">
          <div>
            <span>Fundamental score</span>
            <strong>{score ? score.fundamentalScore : "N/A"}</strong>
          </div>
        </article>

        <article className="metric-card">
          <div>
            <span>Technical score</span>
            <strong>
              {score && score.technicalScore !== null
                ? score.technicalScore
                : "N/A"}
            </strong>
          </div>
        </article>

        <article className="metric-card">
          <div>
            <span>Final score</span>
            <strong>
              {score ? score.finalScore : "N/A"}
              {score && (
                <small>
                  {" "}
                  / {score.maximumScore} &middot; {score.rating}
                </small>
              )}
            </strong>
          </div>
        </article>
      </section>

      {score && !score.technicalScoreAvailable && (
        <section className="partial-notice">
          <Info size={19} />
          <p>
            {score.technicalScoreNote ??
              "Technical score unavailable."}{" "}
            This result reflects fundamentals only and is scored out of{" "}
            {score.maximumScore}, so it is not directly comparable with a
            fully scored company.
          </p>
        </section>
      )}

      {momentum && (
        <section
          className={
            isUptrend ? "momentum-banner up" : "momentum-banner down"
          }
        >
          {isUptrend ? (
            <TrendingUp size={22} />
          ) : (
            <TrendingDown size={22} />
          )}

          <div>
            <span>Price momentum</span>
            <strong>
              {momentum.priceChangePercent > 0 ? "+" : ""}
              {momentum.priceChangePercent}%
            </strong>
          </div>

          <div className="momentum-detail">
            <p>
              First close <strong>{momentum.firstClose}</strong>
            </p>
            <p>
              Latest close <strong>{momentum.latestClose}</strong>
            </p>
            <p className="trend-tag">{momentum.trend}</p>
          </div>
        </section>
      )}

      <section className="chart-panel">
        <div className="section-heading">
          <div>
            <p className="section-kicker">Market behaviour</p>
            <h3>Stored price history</h3>
          </div>

          <span>{chartData.length} trading days</span>
        </div>

        {priceNotice && (
          <div className="development-notice">
            <ShieldAlert size={19} />
            <p>{priceNotice}</p>
          </div>
        )}

        {chartData.length > 0 && (
          <div className="chart-frame">
            <ResponsiveContainer width="100%" height={330}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient
                    id="closeGradient"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop
                      offset="0%"
                      stopColor="#8b5cf6"
                      stopOpacity={0.45}
                    />
                    <stop
                      offset="100%"
                      stopColor="#8b5cf6"
                      stopOpacity={0}
                    />
                  </linearGradient>
                </defs>

                <CartesianGrid
                  stroke="#222a3a"
                  strokeDasharray="4 4"
                  vertical={false}
                />

                <XAxis
                  dataKey="date"
                  stroke="#6f7b90"
                  tickLine={false}
                  axisLine={false}
                  fontSize={11}
                  minTickGap={25}
                />

                <YAxis
                  stroke="#6f7b90"
                  tickLine={false}
                  axisLine={false}
                  fontSize={11}
                  domain={["auto", "auto"]}
                  width={60}
                />

                <Tooltip
                  contentStyle={{
                    background: "#111622",
                    border: "1px solid #2a3243",
                    borderRadius: 12,
                    color: "#e8ecf5",
                    fontSize: 12,
                  }}
                />

                <Area
                  type="monotone"
                  dataKey="close"
                  stroke="#a78bfa"
                  strokeWidth={2}
                  fill="url(#closeGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>

      <section className="development-notice">
        <ShieldAlert size={19} />
        <p>
          Atlas is a research tool in development. Scores combine live price
          signals with development fundamental fixtures and are not
          investment recommendations.
        </p>
      </section>
    </main>
  );
}

export default CompanyPage;
