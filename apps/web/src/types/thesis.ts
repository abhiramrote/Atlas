export interface ThesisCondition {
  id: string;
  metric: string;
  comparison: string;
  threshold: number;
  description: string | null;
  breached: boolean;
  breachedAt: string | null;
  breachedValue: number | null;
}

export interface ThesisVersion {
  id: string;
  versionNumber: number;
  horizonMonths: number;
  conviction: string;
  rationale: string;
  keyRisks: string | null;
  snapshotFundamentalScore: number | null;
  snapshotTechnicalScore: number | null;
  snapshotFinalScore: number | null;
  snapshotRating: string | null;
  snapshotPolicyVersion: string | null;
  snapshotClosePrice: number | null;
  publishedAt: string;
  supersededAt: string | null;
  superseded: boolean;
  invalidationConditions: ThesisCondition[];
}

export interface Thesis {
  id: string;
  companyId: string;
  symbol: string;
  companyName: string;
  title: string;
  state: string;
  currentVersion: number;
  openedAt: string;
  closedAt: string | null;
  horizonEndsOn: string | null;
  allowedTransitions: string[];
  currentVersionDetail: ThesisVersion | null;
}

export interface ThesisEvent {
  id: string;
  eventType: string;
  fromState: string | null;
  toState: string | null;
  versionNumber: number | null;
  detail: string | null;
  occurredAt: string;
}

export interface ConditionInput {
  metric: string;
  comparison: string;
  threshold: number;
  description: string;
}

export interface PublishThesisInput {
  companyId: string;
  title: string;
  horizonMonths: number;
  conviction: string;
  rationale: string;
  keyRisks: string;
  invalidationConditions: ConditionInput[];
}
