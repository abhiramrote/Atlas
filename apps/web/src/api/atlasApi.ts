import type { Opportunity } from "../types/atlas";

export async function getOpportunities(): Promise<Opportunity[]> {
  const response = await fetch("/api/opportunities?limit=20");

  if (!response.ok) {
    throw new Error(
      `Atlas API returned status ${response.status}`
    );
  }

  return response.json() as Promise<Opportunity[]>;
}