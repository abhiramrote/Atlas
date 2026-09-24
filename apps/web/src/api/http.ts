import { getToken, clearToken } from "./auth";

/**
 * Fetch wrapper that attaches the bearer token when present.
 *
 * Public endpoints (rankings, company pages, scores) work fine
 * without a token, so this never blocks a request for lacking one.
 * It only adds the header when a token exists, and clears a token
 * that the server has rejected as expired or invalid so the next
 * request does not keep resending it.
 */
export async function authorizedFetch(
  input: string,
  init: RequestInit = {}
): Promise<Response> {
  const token = getToken();

  const headers = new Headers(init.headers);

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(input, {
    ...init,
    headers,
  });

  if (response.status === 401 && token) {
    // The server has rejected a token we believed was valid. Clear
    // it so the UI reflects signed-out state on the next check
    // rather than repeatedly sending a dead token.
    clearToken();
  }

  return response;
}
