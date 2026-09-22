import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarClock,
  CheckCircle2,
  History,
  Lock,
  ShieldAlert,
} from "lucide-react";

import {
  getAuditTrail,
  getThesis,
  getVersionHistory,
  transitionThesis,
} from "../api/thesisApi";

import ThesisStateBadge from "../components/ThesisStateBadge";

import type {
  Thesis,
  ThesisEvent,
  ThesisVersion,
} from "../types/thesis";

function formatDateTime(value: string | null): string {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function describeCondition(
  metric: string,
  comparison: string,
  threshold: number
): string {
  const readable = metric.replace(/_/g, " ").toLowerCase();

  const operator = {
    BELOW: "falls below",
    ABOVE: "rises above",
    AT_OR_BELOW: "reaches or falls below",
    AT_OR_ABOVE: "reaches or rises above",
  }[comparison] ?? comparison.toLowerCase();

  return `Invalidated if ${readable} ${operator} ${threshold}`;
}

function ThesisDetailPage() {
  const { thesisId } = useParams<{ thesisId: string }>();

  const [thesis, setThesis] = useState<Thesis | null>(null);
  const [versions, setVersions] = useState<ThesisVersion[]>([]);
  const [events, setEvents] = useState<ThesisEvent[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [pendingState, setPendingState] = useState<string | null>(
    null
  );
  const [transitionReason, setTransitionReason] = useState("");
  const [transitionError, setTransitionError] = useState<
    string | null
  >(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    if (!thesisId) {
      setError("No thesis selected");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const [thesisData, versionData, eventData] =
        await Promise.all([
          getThesis(thesisId),
          getVersionHistory(thesisId),
          getAuditTrail(thesisId),
        ]);

      setThesis(thesisData);
      setVersions(versionData);
      setEvents(eventData);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load thesis"
      );
    } finally {
      setLoading(false);
    }
  }, [thesisId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function submitTransition() {
    if (!thesisId || !pendingState) {
      return;
    }

    if (transitionReason.trim().length < 10) {
      setTransitionError(
        "Give a reason of at least 10 characters. " +
          "The reason is the part you will want later."
      );
      return;
    }

    try {
      setSubmitting(true);
      setTransitionError(null);

      await transitionThesis(
        thesisId,
        pendingState,
        transitionReason.trim()
      );

      setPendingState(null);
      setTransitionReason("");

      await load();
    } catch (requestError) {
      setTransitionError(
        requestError instanceof Error
          ? requestError.message
          : "Transition failed"
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <main className="dashboard-shell">
        <div className="state-card" style={{ marginTop: 60 }}>
          <div className="loader" />
          <p>Loading thesis...</p>
        </div>
      </main>
    );
  }

  if (error || !thesis) {
    return (
      <main className="dashboard-shell">
        <div
          className="state-card error-state"
          style={{ marginTop: 60 }}
        >
          <ShieldAlert size={26} />
          <h4>Thesis unavailable</h4>
          <p>{error ?? "Thesis could not be loaded"}</p>

          <Link className="details-button" to="/theses">
            Back to journal
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-shell">
      <header className="topbar">
        <Link className="back-link" to="/theses">
          <ArrowLeft size={17} />
          Back to journal
        </Link>

        <Link
          className="refresh-button"
          to={`/company/${thesis.companyId}`}
        >
          View company intelligence
        </Link>
      </header>

      <section className="thesis-hero">
        <div className="thesis-hero-top">
          <div className="company-identity">
            <div className="symbol-avatar large">
              {thesis.symbol.slice(0, 2)}
            </div>

            <div>
              <h2>{thesis.title}</h2>
              <p>
                {thesis.symbol} &middot; {thesis.companyName}
              </p>
            </div>
          </div>

          <ThesisStateBadge state={thesis.state} large />
        </div>

        <div className="thesis-horizon">
          <CalendarClock size={15} />
          <span>
            Opened {formatDateTime(thesis.openedAt)}
            {thesis.horizonEndsOn &&
              ` · horizon ends ${thesis.horizonEndsOn}`}
          </span>
        </div>
      </section>

      {thesis.allowedTransitions.length > 0 && (
        <section className="transition-panel">
          <p className="section-kicker">Update state</p>

          <div className="transition-buttons">
            {thesis.allowedTransitions.map((state) => (
              <button
                key={state}
                type="button"
                className={
                  state === pendingState
                    ? "filter-chip active"
                    : "filter-chip"
                }
                onClick={() => {
                  setPendingState(
                    state === pendingState ? null : state
                  );
                  setTransitionError(null);
                }}
              >
                {state}
              </button>
            ))}
          </div>

          {pendingState && (
            <div className="transition-form">
              <label htmlFor="transition-reason">
                Why are you moving this to {pendingState}?
              </label>

              <textarea
                id="transition-reason"
                rows={3}
                value={transitionReason}
                placeholder="What evidence changed your assessment?"
                onChange={(event) =>
                  setTransitionReason(event.target.value)
                }
              />

              {transitionError && (
                <p className="form-error">{transitionError}</p>
              )}

              <button
                type="button"
                className="primary-button"
                disabled={submitting}
                onClick={() => void submitTransition()}
              >
                {submitting
                  ? "Recording..."
                  : `Move to ${pendingState}`}
              </button>
            </div>
          )}
        </section>
      )}

      <section className="version-section">
        <div className="section-heading">
          <div>
            <p className="section-kicker">Version history</p>
            <h3>Frozen reasoning</h3>
          </div>

          <span>
            {versions.length} version
            {versions.length === 1 ? "" : "s"}
          </span>
        </div>

        <div className="version-list">
          {versions.map((version) => (
            <article
              className={
                version.superseded
                  ? "version-card superseded"
                  : "version-card current"
              }
              key={version.id}
            >
              <div className="version-head">
                <div className="version-label">
                  <Lock size={14} />
                  Version {version.versionNumber}

                  {version.superseded ? (
                    <span className="version-tag">
                      superseded{" "}
                      {formatDateTime(version.supersededAt)}
                    </span>
                  ) : (
                    <span className="version-tag current-tag">
                      current
                    </span>
                  )}
                </div>

                <span className="version-date">
                  {formatDateTime(version.publishedAt)}
                </span>
              </div>

              <div className="version-meta">
                <div>
                  <span>Conviction</span>
                  <strong>{version.conviction}</strong>
                </div>

                <div>
                  <span>Horizon</span>
                  <strong>
                    {version.horizonMonths} months
                  </strong>
                </div>

                <div>
                  <span>Score at publish</span>
                  <strong>
                    {version.snapshotFinalScore ?? "—"}
                    {version.snapshotRating &&
                      ` · ${version.snapshotRating}`}
                  </strong>
                </div>

                <div>
                  <span>Price at publish</span>
                  <strong>
                    {version.snapshotClosePrice ?? "—"}
                  </strong>
                </div>
              </div>

              <div className="version-body">
                <h5>Rationale</h5>
                <p>{version.rationale}</p>

                {version.keyRisks && (
                  <>
                    <h5>Key risks</h5>
                    <p>{version.keyRisks}</p>
                  </>
                )}

                <h5>
                  Invalidation conditions (
                  {version.invalidationConditions.length})
                </h5>

                <div className="condition-list">
                  {version.invalidationConditions.map(
                    (condition) => (
                      <div
                        className={
                          condition.breached
                            ? "condition breached"
                            : "condition"
                        }
                        key={condition.id}
                      >
                        {condition.breached ? (
                          <ShieldAlert size={15} />
                        ) : (
                          <CheckCircle2 size={15} />
                        )}

                        <div>
                          <strong>
                            {describeCondition(
                              condition.metric,
                              condition.comparison,
                              condition.threshold
                            )}
                          </strong>

                          {condition.description && (
                            <small>
                              {condition.description}
                            </small>
                          )}

                          {condition.breached && (
                            <small className="breach-detail">
                              Breached at{" "}
                              {condition.breachedValue} on{" "}
                              {formatDateTime(
                                condition.breachedAt
                              )}
                            </small>
                          )}
                        </div>
                      </div>
                    )
                  )}
                </div>
              </div>

              {version.snapshotPolicyVersion && (
                <div className="version-footer">
                  Scoring policy{" "}
                  {version.snapshotPolicyVersion}
                </div>
              )}
            </article>
          ))}
        </div>
      </section>

      <section className="audit-section">
        <div className="section-heading">
          <div>
            <p className="section-kicker">Audit trail</p>
            <h3>What happened, in order</h3>
          </div>

          <span>{events.length} events</span>
        </div>

        <div className="timeline">
          {events.map((event) => (
            <div className="timeline-item" key={event.id}>
              <div className="timeline-marker">
                <History size={13} />
              </div>

              <div className="timeline-body">
                <div className="timeline-head">
                  <strong>{event.eventType}</strong>

                  {event.fromState && event.toState && (
                    <span className="timeline-states">
                      {event.fromState} → {event.toState}
                    </span>
                  )}

                  {event.versionNumber && (
                    <span className="version-tag">
                      v{event.versionNumber}
                    </span>
                  )}
                </div>

                {event.detail && <p>{event.detail}</p>}

                <small>
                  {formatDateTime(event.occurredAt)}
                </small>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="development-notice">
        <ShieldAlert size={19} />
        <p>
          Published versions cannot be edited. Revising creates a new
          version so your original reasoning stays inspectable. This
          is a research record, not investment advice.
        </p>
      </section>
    </main>
  );
}

export default ThesisDetailPage;
