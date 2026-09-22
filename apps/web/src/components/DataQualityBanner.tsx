import { useState } from "react";
import { AlertTriangle, ChevronDown, ChevronUp } from "lucide-react";

import type { DataQualityReport } from "../types/atlas";

interface DataQualityBannerProps {
  report: DataQualityReport;
}

/**
 * Surfaces data quality problems above the score.
 *
 * Placed before the metrics deliberately. A user who sees a number
 * first anchors on it, and a caveat below carries far less weight
 * than one that arrives before the figure does.
 *
 * Nothing is hidden. The individual issues are collapsible but
 * present, because "trust us, the data is bad" is less useful than
 * showing which period and which figure.
 */
function DataQualityBanner({ report }: DataQualityBannerProps) {
  const [expanded, setExpanded] = useState(false);

  if (report.issueCount === 0) {
    return null;
  }

  const blocking = !report.reliableForScoring;

  return (
    <section
      className={
        blocking
          ? "quality-banner blocking"
          : "quality-banner advisory"
      }
    >
      <div className="quality-head">
        <AlertTriangle size={19} />

        <div className="quality-copy">
          <strong>
            {blocking
              ? "Scores for this company are unreliable"
              : "Data quality notes"}
          </strong>

          <p>{report.summary}</p>
        </div>

        <button
          type="button"
          className="quality-toggle"
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? (
            <ChevronUp size={16} />
          ) : (
            <ChevronDown size={16} />
          )}
          {report.issueCount} issue
          {report.issueCount === 1 ? "" : "s"}
        </button>
      </div>

      {expanded && (
        <div className="quality-issues">
          {report.issues.map((issue, index) => (
            <div
              className={
                issue.severity === "ERROR"
                  ? "quality-issue error"
                  : "quality-issue warning"
              }
              key={`${issue.check}-${issue.fiscalYear}-${index}`}
            >
              <div className="quality-issue-head">
                <span className="issue-metric">
                  {issue.metric.replace(/_/g, " ")}
                </span>

                <span className="issue-year">
                  FY{issue.fiscalYear}
                </span>

                <span
                  className={
                    issue.severity === "ERROR"
                      ? "issue-severity error"
                      : "issue-severity warning"
                  }
                >
                  {issue.severity}
                </span>
              </div>

              <p className="issue-observed">
                Observed{" "}
                <strong>
                  {issue.observedValue ?? "unavailable"}
                </strong>
                {" · expected "}
                {issue.expectedRange}
              </p>

              <p className="issue-explanation">
                {issue.explanation}
              </p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default DataQualityBanner;
