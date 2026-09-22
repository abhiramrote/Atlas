import type {
  CombinedScore,
  Company,
  DataQualityReport,
  FinancialGrowth,
  Momentum,
  Opportunity,
  PriceBar,
  RiskMetrics,
  Scoreability,
  SectorComparison,
} from "../types/atlas";

/**
 * Reads the server error message when present so validation
 * failures explain themselves rather than showing a status code.
 */
async function request<T>(url: string): Promise<T> {
  const response = await fetch(url);

  if (!response.ok) {
    let message = `Atlas API returned status ${response.status}`;

    try {
      const body = await response.json();

      if (body && typeof body.message === "string") {
        message = body.message;
      }
    } catch {
      // No JSON body. Keep the status message.
    }

    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export function getOpportunities(
  limit = 50
): Promise<Opportunity[]> {
  return request<Opportunity[]>(
    `/api/opportunities?limit=${limit}`
  );
}

export function getCompany(companyId: string): Promise<Company> {
  return request<Company>(`/api/companies/${companyId}`);
}

export function getCombinedScore(
  companyId: string
): Promise<CombinedScore> {
  return request<CombinedScore>(
    `/api/opportunities/v2/${companyId}`
  );
}

export function getMomentum(
  instrumentId: string
): Promise<Momentum> {
  return request<Momentum>(`/api/momentum/${instrumentId}`);
}

export function getPrices(
  instrumentId: string
): Promise<PriceBar[]> {
  return request<PriceBar[]>(`/api/prices/${instrumentId}`);
}

export function getRiskMetrics(
  instrumentId: string
): Promise<RiskMetrics> {
  return request<RiskMetrics>(`/api/risk/${instrumentId}`);
}

export function getDataQuality(
  companyId: string
): Promise<DataQualityReport> {
  return request<DataQualityReport>(
    `/api/data-quality/companies/${companyId}`
  );
}

export function getScoreability(
  companyId: string
): Promise<Scoreability> {
  return request<Scoreability>(
    `/api/sectors/companies/${companyId}/scoreability`
  );
}

export function getSectorComparison(
  companyId: string
): Promise<SectorComparison> {
  return request<SectorComparison>(
    `/api/sectors/companies/${companyId}/comparison`
  );
}

export function getGrowth(
  companyId: string
): Promise<FinancialGrowth> {
  return request<FinancialGrowth>(
    `/api/growth/company/${companyId}`
  );
}
