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
  companyId: string;
  symbol: string;
  companyName: string;
  fundamentalScore: number;
  technicalScore: number | null;
  finalScore: number;
  maximumScore: number;
  rating: string;
  policyVersion: string;
  technicalScoreAvailable: boolean;
  technicalScoreNote: string | null;
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

export interface RiskMetrics {
  instrumentId: string;
  symbol: string;
  observationDays: number;
  annualisedVolatilityPercent: number | null;
  maximumDrawdownPercent: number | null;
  peakClose: number | null;
  troughClose: number | null;
  riskBand: string;
  note: string;
}

export interface DataQualityIssue {
  check: string;
  severity: string;
  fiscalYear: number;
  metric: string;
  observedValue: number | null;
  expectedRange: string;
  explanation: string;
}

export interface DataQualityReport {
  companyId: string;
  symbol: string;
  periodsChecked: number;
  issueCount: number;
  errorCount: number;
  reliableForScoring: boolean;
  summary: string;
  issues: DataQualityIssue[];
}

export interface Scoreability {
  companyId: string;
  symbol: string;
  companyName: string;
  scoringProfile: string;
  scoreable: boolean;
  reason: string;
}

export interface SectorComparison {
  sector: string;
  sectorPeerCount: number;
  baselineSufficient: boolean;
  companyOperatingMargin: number | null;
  sectorMedianOperatingMargin: number | null;
  relativeOperatingMargin: number | null;
  companyProfitMargin: number | null;
  sectorMedianProfitMargin: number | null;
  relativeProfitMargin: number | null;
  note: string | null;
}

export interface FinancialGrowth {
  companyId: string;
  symbol: string;
  currentFiscalYear: number;
  previousFiscalYear: number;
  currentRevenue: number | null;
  previousRevenue: number | null;
  revenueGrowthPercent: number | null;
  netIncomeGrowthPercent: number | null;
  operatingProfitGrowthPercent: number | null;
  operatingCashFlowGrowthPercent: number | null;
}
