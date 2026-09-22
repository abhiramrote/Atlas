import { useEffect, useState } from "react";
import { Plus, ShieldAlert, Trash2, X } from "lucide-react";

import { getOpportunities } from "../api/atlasApi";
import { publishThesis } from "../api/thesisApi";

import type { Opportunity } from "../types/atlas";
import type { ConditionInput } from "../types/thesis";

interface PublishThesisModalProps {
  onClose: () => void;
  onPublished: () => void;
  presetCompanyId?: string;
}

const METRICS = [
  "OPERATING_MARGIN",
  "PROFIT_MARGIN",
  "REVENUE_GROWTH",
  "NET_INCOME_GROWTH",
  "OPERATING_CASH_FLOW_GROWTH",
  "MAX_DRAWDOWN",
  "VOLATILITY",
  "FINAL_SCORE",
];

const COMPARISONS = [
  "BELOW",
  "ABOVE",
  "AT_OR_BELOW",
  "AT_OR_ABOVE",
];

const MINIMUM_RATIONALE = 40;

function emptyCondition(): ConditionInput {
  return {
    metric: "OPERATING_MARGIN",
    comparison: "BELOW",
    threshold: 0,
    description: "",
  };
}

function PublishThesisModal({
  onClose,
  onPublished,
  presetCompanyId,
}: PublishThesisModalProps) {
  const [companies, setCompanies] = useState<Opportunity[]>([]);

  const [companyId, setCompanyId] = useState(
    presetCompanyId ?? ""
  );
  const [title, setTitle] = useState("");
  const [horizonMonths, setHorizonMonths] = useState(18);
  const [conviction, setConviction] = useState("MEDIUM");
  const [rationale, setRationale] = useState("");
  const [keyRisks, setKeyRisks] = useState("");

  const [conditions, setConditions] = useState<ConditionInput[]>([
    emptyCondition(),
  ]);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getOpportunities()
      .then((data) => {
        setCompanies(data);

        if (!presetCompanyId && data.length > 0) {
          setCompanyId(data[0].companyId);
        }
      })
      .catch(() => {
        setError(
          "Could not load companies. Is the Atlas API running?"
        );
      });
  }, [presetCompanyId]);

  function updateCondition(
    index: number,
    patch: Partial<ConditionInput>
  ) {
    setConditions((current) =>
      current.map((condition, position) =>
        position === index
          ? { ...condition, ...patch }
          : condition
      )
    );
  }

  function removeCondition(index: number) {
    setConditions((current) =>
      current.filter((_, position) => position !== index)
    );
  }

  async function submit() {
    if (!companyId) {
      setError("Select a company");
      return;
    }

    if (title.trim().length === 0) {
      setError("Give the thesis a title");
      return;
    }

    if (rationale.trim().length < MINIMUM_RATIONALE) {
      setError(
        `Rationale needs at least ${MINIMUM_RATIONALE} characters. ` +
          "Write what you actually believe and why."
      );
      return;
    }

    if (conditions.length === 0) {
      setError(
        "Add at least one invalidation condition. A thesis that " +
          "cannot be proven wrong is not a thesis."
      );
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      await publishThesis({
        companyId,
        title: title.trim(),
        horizonMonths,
        conviction,
        rationale: rationale.trim(),
        keyRisks: keyRisks.trim(),
        invalidationConditions: conditions.map((condition) => ({
          ...condition,
          threshold: Number(condition.threshold),
        })),
      });

      onPublished();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not publish thesis"
      );
    } finally {
      setSubmitting(false);
    }
  }

  const rationaleRemaining =
    MINIMUM_RATIONALE - rationale.trim().length;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-head">
          <div>
            <p className="section-kicker">New thesis</p>
            <h3>Commit your reasoning</h3>
          </div>

          <button
            type="button"
            className="icon-button"
            onClick={onClose}
          >
            <X size={18} />
          </button>
        </div>

        <div className="modal-body">
          <div className="field">
            <label htmlFor="company">Company</label>

            <select
              id="company"
              value={companyId}
              onChange={(event) =>
                setCompanyId(event.target.value)
              }
            >
              {companies.map((company) => (
                <option
                  key={company.companyId}
                  value={company.companyId}
                >
                  {company.symbol} — {company.companyName}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label htmlFor="title">Title</label>

            <input
              id="title"
              type="text"
              value={title}
              placeholder="e.g. Margin expansion thesis"
              onChange={(event) =>
                setTitle(event.target.value)
              }
            />
          </div>

          <div className="field-row">
            <div className="field">
              <label htmlFor="horizon">
                Horizon (months)
              </label>

              <input
                id="horizon"
                type="number"
                min={1}
                max={120}
                value={horizonMonths}
                onChange={(event) =>
                  setHorizonMonths(
                    Number(event.target.value)
                  )
                }
              />
            </div>

            <div className="field">
              <label htmlFor="conviction">Conviction</label>

              <select
                id="conviction"
                value={conviction}
                onChange={(event) =>
                  setConviction(event.target.value)
                }
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>
          </div>

          <div className="field">
            <label htmlFor="rationale">
              Why do you think this works?
            </label>

            <textarea
              id="rationale"
              rows={5}
              value={rationale}
              placeholder="Be specific. Numbers and mechanisms, not adjectives."
              onChange={(event) =>
                setRationale(event.target.value)
              }
            />

            <small
              className={
                rationaleRemaining > 0
                  ? "hint warning"
                  : "hint"
              }
            >
              {rationaleRemaining > 0
                ? `${rationaleRemaining} more characters needed`
                : "Length requirement met"}
            </small>
          </div>

          <div className="field">
            <label htmlFor="risks">
              Key risks (optional)
            </label>

            <textarea
              id="risks"
              rows={3}
              value={keyRisks}
              placeholder="What could go wrong that you are accepting?"
              onChange={(event) =>
                setKeyRisks(event.target.value)
              }
            />
          </div>

          <div className="field">
            <div className="conditions-head">
              <label>What would prove you wrong?</label>

              <button
                type="button"
                className="ghost-button"
                onClick={() =>
                  setConditions((current) => [
                    ...current,
                    emptyCondition(),
                  ])
                }
              >
                <Plus size={14} />
                Add condition
              </button>
            </div>

            <small className="hint">
              At least one is required. This is the part that makes
              the thesis honest.
            </small>

            {conditions.map((condition, index) => (
              <div className="condition-row" key={index}>
                <select
                  value={condition.metric}
                  onChange={(event) =>
                    updateCondition(index, {
                      metric: event.target.value,
                    })
                  }
                >
                  {METRICS.map((metric) => (
                    <option key={metric} value={metric}>
                      {metric.replace(/_/g, " ")}
                    </option>
                  ))}
                </select>

                <select
                  value={condition.comparison}
                  onChange={(event) =>
                    updateCondition(index, {
                      comparison: event.target.value,
                    })
                  }
                >
                  {COMPARISONS.map((comparison) => (
                    <option
                      key={comparison}
                      value={comparison}
                    >
                      {comparison.replace(/_/g, " ")}
                    </option>
                  ))}
                </select>

                <input
                  type="number"
                  step="0.01"
                  value={condition.threshold}
                  onChange={(event) =>
                    updateCondition(index, {
                      threshold: Number(
                        event.target.value
                      ),
                    })
                  }
                />

                <input
                  type="text"
                  placeholder="Why this matters"
                  value={condition.description}
                  onChange={(event) =>
                    updateCondition(index, {
                      description: event.target.value,
                    })
                  }
                />

                {conditions.length > 1 && (
                  <button
                    type="button"
                    className="icon-button"
                    onClick={() => removeCondition(index)}
                  >
                    <Trash2 size={15} />
                  </button>
                )}
              </div>
            ))}
          </div>

          {error && (
            <div className="form-error-panel">
              <ShieldAlert size={17} />
              <p>{error}</p>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <p className="hint">
            Once published this version is frozen. Revisions create
            new versions.
          </p>

          <div className="modal-actions">
            <button
              type="button"
              className="ghost-button"
              onClick={onClose}
            >
              Cancel
            </button>

            <button
              type="button"
              className="primary-button"
              disabled={submitting}
              onClick={() => void submit()}
            >
              {submitting ? "Publishing..." : "Publish thesis"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default PublishThesisModal;
