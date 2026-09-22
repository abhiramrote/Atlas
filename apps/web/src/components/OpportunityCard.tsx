import { Link } from "react-router-dom";
import { AlertTriangle, ArrowUpRight } from "lucide-react";

import type { Opportunity } from "../types/atlas";

interface OpportunityCardProps {
  opportunity: Opportunity;
  rank: number;
  dataReliable?: boolean;
}

function ratingClass(rating: string): string {
  switch (rating.toUpperCase()) {
    case "ELITE":
    case "STRONG":
      return "rating rating-strong";
    case "GOOD":
      return "rating rating-good";
    case "MODERATE":
      return "rating rating-moderate";
    default:
      return "rating rating-weak";
  }
}

/**
 * One ranked opportunity.
 *
 * When the underlying data is unreliable the score is visually
 * muted and a warning badge sits beside the symbol. Showing the
 * number normally would let a reader anchor on it before noticing
 * the caveat, which is the anchoring problem the company page
 * already solves by placing the warning above the metrics.
 *
 * dataReliable defaults to true so the card still renders correctly
 * if the overview call failed. Defaulting to a warning would flood
 * the dashboard on any transient problem.
 */
function OpportunityCard({
  opportunity,
  rank,
  dataReliable = true,
}: OpportunityCardProps) {

  const safeScore = Math.max(
    0,
    Math.min(opportunity.score, opportunity.maximumScore)
  );

  const percentage =
    opportunity.maximumScore > 0
      ? Math.round((safeScore / opportunity.maximumScore) * 100)
      : 0;

  return (
    <article
      className={
        dataReliable
          ? "opportunity-card"
          : "opportunity-card unreliable"
      }
    >
      <div className="opportunity-header">
        <div className="rank">#{rank}</div>

        <div className="company-identity">
          <div className="symbol-avatar">
            {opportunity.symbol.slice(0, 2)}
          </div>

          <div className="company-text">
            <h4>
              {opportunity.symbol}

              {!dataReliable && (
                <span
                  className="unreliable-badge"
                  title="Underlying financial data has quality issues"
                >
                  <AlertTriangle size={11} />
                </span>
              )}
            </h4>

            <p>{opportunity.companyName}</p>
          </div>
        </div>

        <span className={ratingClass(opportunity.rating)}>
          {opportunity.rating}
        </span>
      </div>

      <div className="score-row">
        <div>
          <span>Opportunity score</span>
          <strong
            className={dataReliable ? "" : "score-muted"}
          >
            {opportunity.score}
            <small>/{opportunity.maximumScore}</small>
          </strong>
        </div>

        <div
          className={
            dataReliable
              ? "score-ring"
              : "score-ring score-ring-muted"
          }
          style={{
            background: `conic-gradient(${
              dataReliable ? "#8b5cf6" : "#5b6577"
            } ${percentage}%, #202738 ${percentage}% 100%)`,
          }}
        >
          <div>{percentage}%</div>
        </div>
      </div>

      {!dataReliable && (
        <p className="unreliable-note">
          Score rests on data with known quality issues
        </p>
      )}

      <div className="progress-track">
        <div
          className={
            dataReliable
              ? "progress-fill"
              : "progress-fill progress-muted"
          }
          style={{ width: `${percentage}%` }}
        />
      </div>

      <div className="period-row">
        <span>Compared periods</span>
        <strong>
          FY {opportunity.previousFiscalYear}
          <ArrowUpRight size={15} />
          FY {opportunity.currentFiscalYear}
        </strong>
      </div>

      <div className="factor-list">
        {opportunity.factors.slice(0, 3).map((factor) => (
          <div className="factor" key={factor.name}>
            <div className="factor-copy">
              <span>{factor.name}</span>
              <small>{factor.explanation}</small>
            </div>

            <strong>
              {factor.pointsAwarded}/{factor.maximumPoints}
            </strong>
          </div>
        ))}
      </div>

      <Link
        className="details-button"
        to={`/company/${opportunity.companyId}`}
      >
        View company intelligence
        <ArrowUpRight size={16} />
      </Link>
    </article>
  );
}

export default OpportunityCard;
