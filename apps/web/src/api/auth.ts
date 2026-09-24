/**
 * Auth API client and token storage.
 *
 * WHY LOCALSTORAGE, NOT COOKIES
 *
 * Atlas's backend issues a bearer JWT, not a session cookie. Storing
 * it in localStorage keeps the frontend framework-agnostic and avoids
 * CSRF entirely, since the token must be explicitly attached to each
 * request rather than sent automatically by the browser.
 *
 * The tradeoff is XSS exposure: any injected script can read
 * localStorage. Atlas accepts this because React escapes rendered
 * content by default and the app has no user-generated HTML
 * rendering path. If that ever changes, httpOnly cookies would be
 * the safer choice.
 */

const TOKEN_KEY = "atlas_token";

export interface CurrentUser {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  role: string;
  isAdmin: boolean;
  lastLoginAt: string | null;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

/**
 * Starts the OAuth2 login flow.
 *
 * This is a full page redirect, not a fetch call, because the OAuth2
 * authorization endpoint on the backend itself redirects to Google.
 * A fetch cannot follow that redirect chain across origins the way
 * the browser's address bar can.
 */
export function redirectToGoogleLogin(): void {
  const apiBase = import.meta.env.VITE_API_BASE_URL ?? "";
  window.location.href = `${apiBase}/oauth2/authorization/google`;
}

export function logout(): void {
  clearToken();
  window.location.href = "/";
}

/**
 * Fetches the current user using the stored token.
 *
 * Returns null on any failure rather than throwing. An expired or
 * missing token is an ordinary state for this call, not an
 * exceptional one, and every page that checks auth status would
 * otherwise need a try/catch around it.
 */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  const token = getToken();

  if (!token) {
    return null;
  }

  try {
    const apiBase = import.meta.env.VITE_API_BASE_URL ?? "";

    const response = await fetch(`${apiBase}/api/auth/me`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.status === 204) {
      // Backend recognises the request but reports no authenticated
      // user. Treat the stored token as stale.
      clearToken();
      return null;
    }

    if (!response.ok) {
      clearToken();
      return null;
    }

    return (await response.json()) as CurrentUser;
  } catch {
    return null;
  }
}
