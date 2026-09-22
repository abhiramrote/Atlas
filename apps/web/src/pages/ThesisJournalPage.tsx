import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  ArrowUpRight,
  CalendarClock,
  FileText,
  Plus,
  ShieldAlert,
} from "lucide-react";

import { getTheses } from "../api/thesisApi";
import ThesisStateBadge from "../components/ThesisStateBadge";
import PublishThesisModal from "../components/PublishThesisModal";

import type { Thesis } from "../types/thesis";

function formatDate(value: string | null): string {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function ThesisJournalPage() {
  const [theses, setTheses] = useState<Thesis[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [stateFilter, setStateFilter] = useState<string>("ALL");

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await getTheses();
      setTheses(data);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load theses"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const states = useMemo(() => {
    const unique = new Set(theses.map((item) => item.state));
    return ["ALL", ...Array.from(unique).sort()];
  }, [theses]);

  const visible = useMemo(() => {
    if (stateFilter === "ALL") {
      return theses;
    }

    return theses.filter((item) => item.state === stateFilter);
  }, [theses, stateFilter]);

  const openCount = theses.filter(
    (item) => item.state !== "ARCHIVED"
  ).length;

  const invalidatedCount = theses.filter(
    (item) => item.state === "INVALIDATED"
  ).length;

  const breachedCount = theses.filter((item) =>
    item.currentVersionDetail?.invalidationConditions.some(
      (condition) => condition.breached
    )
  ).length;

  return (
    <main className="dashboard-shell">
      <header className="topbar">
        <Link className="back-link" to="/">
          <ArrowLeft size={17} />
          Back to dashboard
        </Link>

        <button
          className="refresh-button"
          type="button"
          onClick={() => setModalOpen(true)}
        >
          <Plus size={16} />
          New thesis
        </button>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <div className="eyebrow">
            <FileText size={15} />
            Research journal
          </div>

          <h2>
            What you believed,
            <span> and whether you were right.</span>
          </h2>

          <p>
            Every published thesis is frozen with its reasoning,
            invalidation conditions and the scores visible at the
            time. Revisions create new versions rather than
            overwriting the original judgement.
          </p>
        </div>

        <div className="live-card">
          <div className="live-indicator">
            <span />
            Journal active
          </div>

          <p>Open theses</p>
          <strong>{openCount}</strong>
        </div>
      </section>

      <section className="metric-grid">
        <article className="metric-card">
          <div>
            <span>Total theses</span>
            <strong>{theses.length}</strong>
          </div>
        </article>

        <article className="metric-card">
          <div>
            <span>Invalidated</span>
            <strong>{invalidatedCount}</strong>
          </div>
        </article>

        <article className="metric-card">
          <div>
            <span>With breached conditions</span>
            <strong>{breachedCount}</strong>
          </div>
        </article>
      </section>

      {theses.length > 0 && (
        <div className="filter-row">
          {states.map((state) => (
            <button
              key={state}
              type="button"
              className={
                state === stateFilter
                  ? "filter-chip active"
                  : "filter-chip"
              }
              onClick={() => setStateFilter(state)}
            >
              {state}
            </button>
          ))}
        </div>
      )}

      <section className="opportunity-section">
        {loading && (
          <div className="state-card">
            <div className="loader" />
            <p>Loading research journal...</p>
          </div>
        )}

        {!loading && error && (
          <div className="state-card error-state">
            <ShieldAlert size={26} />
            <h4>Journal unavailable</h4>
            <p>{error}</p>

            <button type="button" onClick={() => void load()}>
              Try again
            </button>
          </div>
        )}

        {!loading && !error && theses.length === 0 && (
          <div className="state-card">
            <FileText size={25} />
            <h4>No theses yet</h4>
            <p>
              Publish your first thesis to start building a record
              of your reasoning.
            </p>

            <button
              type="button"
              onClick={() => setModalOpen(true)}
            >
              Write first thesis
            </button>
          </div>
        )}

        {!loading && !error && visible.length > 0 && (
          <div className="thesis-list">
            {visible.map((thesis) => {
              const detail = thesis.currentVersionDetail;

              const breached =
                detail?.invalidationConditions.filter(
                  (condition) => condition.breached
                ) ?? [];

              return (
                <article
                  className="thesis-card"
                  key={thesis.id}
                >
                  <div className="thesis-card-head">
                    <div className="company-identity">
                      <div className="symbol-avatar">
                        {thesis.symbol.slice(0, 2)}
                      </div>

                      <div className="company-text">
                        <h4>{thesis.title}</h4>
                        <p>
                          {thesis.symbol} &middot;{" "}
                          {thesis.companyName}
                        </p>
                      </div>
                    </div>

                    <ThesisStateBadge state={thesis.state} />
                  </div>

                  {detail && (
                    <p className="thesis-rationale">
                      {detail.rationale}
                    </p>
                  )}

                  <div className="thesis-meta">
                    <div>
                      <span>Version</span>
                      <strong>v{thesis.currentVersion}</strong>
                    </div>

                    <div>
                      <span>Conviction</span>
                      <strong>
                        {detail?.conviction ?? "—"}
                      </strong>
                    </div>

                    <div>
                      <span>Score at publish</span>
                      <strong>
                        {detail?.snapshotFinalScore ?? "—"}
                      </strong>
                    </div>

                    <div>
                      <span>Price at publish</span>
                      <strong>
                        {detail?.snapshotClosePrice ?? "—"}
                      </strong>
                    </div>
                  </div>

                  <div className="thesis-horizon">
                    <CalendarClock size={15} />
                    <span>
                      Published {formatDate(detail?.publishedAt ?? null)}
                      {" · horizon ends "}
                      {formatDate(thesis.horizonEndsOn)}
                    </span>
                  </div>

                  {breached.length > 0 && (
                    <div className="breach-strip">
                      <ShieldAlert size={15} />
                      {breached.length} invalidation condition
                      {breached.length === 1 ? "" : "s"} breached
                    </div>
                  )}

                  <Link
                    className="details-button"
                    to={`/theses/${thesis.id}`}
                  >
                    Open thesis
                    <ArrowUpRight size={16} />
                  </Link>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {modalOpen && (
        <PublishThesisModal
          onClose={() => setModalOpen(false)}
          onPublished={() => {
            setModalOpen(false);
            void load();
          }}
        />
      )}
    </main>
  );
}

export default ThesisJournalPage;
