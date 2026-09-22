import { Activity } from "lucide-react";

import type { RiskMetrics } from "../types/atlas";

interface RiskPanelProps {
  risk: RiskMetrics;
}

function riskClass(band: string): string {
  switch (band.toUpperCase()) {
    case "LOW":
      return "risk-low";
    case "MODERATE":
      return "risk-moderate";
    case "ELEVATED":
      return "risk-elevated";
    case "HIGH":
      return "risk-high";
    default:
      return "risk-unknown";
  }
}

/**
 * Backward looking risk from stored price history.
 *
 * The observation count and caveat note are shown rather than
 * buried. A volatility figure from thirty days is indicative at
 * best, and presenting it without that context would imply more
 * precision than exists.
 */
function RiskPanel({ risk }: RiskPanelProps) {
  return (
    <section className="panel">
      <div className="panel-head">
        <Activity size={17} />
        <h4>Risk</h4>

        <span className={`risk-band ${riskClass(risk.riskBand)}`}>
          {risk.riskBand}
        </span>
      </div>

      <div className="metric-pair">
        <div>
          <span>Annualised volatility</span>
          <strong>
            {risk.annualisedVolatilityPercent ?? "—"}%
          </strong>
        </div>

        <div>
          <span>Maximum drawdown</span>
          <strong className="drawdown">
            {risk.maximumDrawdownPercent ?? "—"}%
          </strong>
        </div>
      </div>

      {risk.peakClose !== null && risk.troughClose !== null && (
        <p className="panel-detail">
          Peak {risk.peakClose} to trough {risk.troughClose} over{" "}
          {risk.observationDays} trading days
        </p>
      )}

      <p className="panel-note">{risk.note}</p>
    </section>
  );
}

export default RiskPanel;