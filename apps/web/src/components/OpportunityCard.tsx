import { ArrowUpRight } from "lucide-react";
import type { Opportunity } from "../types/atlas";

interface OpportunityCardProps {
  opportunity: Opportunity;
  rank: number;
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

function OpportunityCard({ opportunity, rank }: OpportunityCardProps) {
  const safeScore = Math.max(
    0,
    Math.min(opportunity.score, opportunity.maximumScore)
  );

  const percentage =
    opportunity.maximumScore > 0
      ? Math.round((safeScore / opportunity.maximumScore) * 100)
      : 0;

  return (
    <article className="opportunity-card">
      <div className="opportunity-header">
        <div className="rank">#{rank}</div>

        <div className="company-identity">
          <div className="symbol-avatar">
            {opportunity.symbol.slice(0, 2)}
          </div>

          <div className="company-text">
            <h4>{opportunity.symbol}</h4>
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
          <strong>
            {opportunity.score}
            <small>/{opportunity.maximumScore}</small>
          </strong>
        </div>

        <div
          className="score-ring"
          style={{
            background: `conic-gradient(#8b5cf6 ${percentage}%, #202738 ${percentage}% 100%)`,
          }}
        >
          <div>{percentage}%</div>
        </div>
      </div>

      <div className="progress-track">
        <div className="progress-fill" style={{ width: `${percentage}%` }} />
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

      <button className="details-button" type="button">
        View company intelligence
        <ArrowUpRight size={16} />
      </button>
    </article>
  );
}

export default OpportunityCard;
