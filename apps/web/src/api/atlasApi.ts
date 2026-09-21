import type {
  CombinedScore,
  Company,
  Momentum,
  Opportunity,
  PriceBar,
} from "../types/atlas";

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`Atlas API returned status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getOpportunities(): Promise<Opportunity[]> {
  return getJson<Opportunity[]>("/api/opportunities?limit=20");
}

export function getCompany(companyId: string): Promise<Company> {
  return getJson<Company>(`/api/companies/${companyId}`);
}

export function getCombinedScore(
  companyId: string
): Promise<CombinedScore> {
  return getJson<CombinedScore>(`/api/opportunities/v2/${companyId}`);
}

export function getMomentum(instrumentId: string): Promise<Momentum> {
  return getJson<Momentum>(`/api/momentum/${instrumentId}`);
}

export function getPrices(instrumentId: string): Promise<PriceBar[]> {
  return getJson<PriceBar[]>(`/api/prices/${instrumentId}`);
}
