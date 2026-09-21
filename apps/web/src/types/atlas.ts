export interface ScoreFactor {
  name: string;
  value: number | null;
  pointsAwarded: number;
  maximumPoints: number;
  explanation: string;
}

export interface Opportunity {
  companyId: string;
  symbol: string;
  companyName: string;
  score: number;
  maximumScore: number;
  rating: string;
  policyVersion: string;
  currentFiscalYear: number;
  previousFiscalYear: number;
  factors: ScoreFactor[];
}