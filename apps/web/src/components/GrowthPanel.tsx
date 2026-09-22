import { TrendingUp } from "lucide-react";

import type { FinancialGrowth } from "../types/atlas";

interface GrowthPanelProps {
  growth: FinancialGrowth;
}

function GrowthPanel({ growth }: GrowthPanelProps) {

  function renderGrowth(value: number | null) {
    if (value === null) {
      return <strong className="growth-muted">—</strong>;
    }

    return (
      <strong
        className={
          value >= 0 ? "growth-positive" : "growth-negative"
        }
      >
        {value > 0 ? "+" : ""}
        {value}%
      </strong>
    );
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <TrendingUp size={17} />
        <h4>Year on year growth</h4>

        <span className="panel-meta">
          FY{growth.previousFiscalYear} to FY
          {growth.currentFiscalYear}
        </span>
      </div>

      <div className="growth-grid">
        <div>
          <span>Revenue</span>
          {renderGrowth(growth.revenueGrowthPercent)}
        </div>

        <div>
          <span>Net income</span>
          {renderGrowth(growth.netIncomeGrowthPercent)}
        </div>

        <div>
          <span>Operating profit</span>
          {renderGrowth(growth.operatingProfitGrowthPercent)}
        </div>

        <div>
          <span>Operating cash flow</span>
          {renderGrowth(
            growth.operatingCashFlowGrowthPercent
          )}
        </div>
      </div>
    </section>
  );
}

export default GrowthPanel;
