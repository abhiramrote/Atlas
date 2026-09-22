import { Users } from "lucide-react";

import type { SectorComparison } from "../types/atlas";

interface SectorComparisonPanelProps {
  comparison: SectorComparison;
}

/**
 * Shows margins against the sector median.
 *
 * The relative figure is the point. A 21.7 percent operating margin
 * means nothing on its own, because software and cement sit in very
 * different ranges. Against the sector it becomes a judgement.
 *
 * When the baseline has too few peers the relative figure is hidden
 * rather than shown greyed, because a median drawn from one company
 * is that company and displaying it invites misreading.
 */
function SectorComparisonPanel({
  comparison,
}: SectorComparisonPanelProps) {

  const usable = comparison.baselineSufficient;

  function renderRelative(value: number | null) {
    if (!usable || value === null) {
      return <span className="relative-muted">—</span>;
    }

    const positive = value > 0;

    return (
      <span
        className={
          positive ? "relative-above" : "relative-below"
        }
      >
        {positive ? "+" : ""}
        {value}% vs sector
      </span>
    );
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <Users size={17} />
        <h4>Sector comparison</h4>
        <span className="panel-meta">
          {comparison.sector} · {comparison.sectorPeerCount} peer
          {comparison.sectorPeerCount === 1 ? "" : "s"}
        </span>
      </div>

      <div className="comparison-grid">
        <div className="comparison-row">
          <span>Operating margin</span>

          <strong>
            {comparison.companyOperatingMargin ?? "—"}%
          </strong>

          <span className="comparison-median">
            median{" "}
            {comparison.sectorMedianOperatingMargin ?? "—"}%
          </span>

          {renderRelative(comparison.relativeOperatingMargin)}
        </div>

        <div className="comparison-row">
          <span>Profit margin</span>

          <strong>
            {comparison.companyProfitMargin ?? "—"}%
          </strong>

          <span className="comparison-median">
            median{" "}
            {comparison.sectorMedianProfitMargin ?? "—"}%
          </span>

          {renderRelative(comparison.relativeProfitMargin)}
        </div>
      </div>

      {comparison.note && (
        <p className="panel-note">{comparison.note}</p>
      )}
    </section>
  );
}

export default SectorComparisonPanel;