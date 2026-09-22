import type {
  PublishThesisInput,
  Thesis,
  ThesisEvent,
  ThesisVersion,
} from "../types/thesis";

/**
 * Reads the server error message when available, so validation
 * failures surface the actual reason rather than a status code.
 */
async function request<T>(
  url: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(url, options);

  if (!response.ok) {
    let message = `Atlas API returned status ${response.status}`;

    try {
      const body = await response.json();

      if (body && typeof body.message === "string") {
        message = body.message;
      }
    } catch {
      // Response had no JSON body. Keep the status message.
    }

    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export function getTheses(companyId?: string): Promise<Thesis[]> {
  const url = companyId
    ? `/api/theses?companyId=${companyId}`
    : "/api/theses";

  return request<Thesis[]>(url);
}

export function getThesis(thesisId: string): Promise<Thesis> {
  return request<Thesis>(`/api/theses/${thesisId}`);
}

export function getVersionHistory(
  thesisId: string
): Promise<ThesisVersion[]> {
  return request<ThesisVersion[]>(
    `/api/theses/${thesisId}/versions`
  );
}

export function getAuditTrail(
  thesisId: string
): Promise<ThesisEvent[]> {
  return request<ThesisEvent[]>(`/api/theses/${thesisId}/audit`);
}

export function publishThesis(
  input: PublishThesisInput
): Promise<Thesis> {
  return request<Thesis>("/api/theses", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function transitionThesis(
  thesisId: string,
  targetState: string,
  reason: string
): Promise<Thesis> {
  return request<Thesis>(`/api/theses/${thesisId}/transitions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ targetState, reason }),
  });
}
