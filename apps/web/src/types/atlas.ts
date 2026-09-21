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

export interface Company {
  id: string;
  instrumentId: string;
  symbol: string;
  companyName: string;
  exchange: string;
  sector: string | null;
  industry: string | null;
  website: string | null;
  description: string | null;
}

export interface CombinedScore {
  symbol: string;
  fundamentalScore: number;
  technicalScore: number;
  finalScore: number;
  rating: string;
}

export interface Momentum {
  symbol: string;
  firstClose: number;
  latestClose: number;
  priceChangePercent: number;
  trend: string;
}

export interface PriceBar {
  tradeDate: string;
  openPrice: number | null;
  highPrice: number | null;
  lowPrice: number | null;
  closePrice: number | null;
  volume: number | null;
}
