package com.abhiram.atlas.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Returns 401 JSON for unauthenticated API requests instead of
 * redirecting to the OAuth2 login page.
 *
 * WHY THIS EXISTS
 *
 * Spring Security's default behaviour when oauth2Login() is enabled
 * is to redirect any unauthenticated request to a protected endpoint
 * toward the login flow. That is correct for a browser navigating to
 * a page, but wrong for a fetch() call from a single page app: the
 * browser follows the 302 directly, bypassing the frontend dev
 * server's proxy, and lands on the backend origin with no CORS
 * headers configured for that path. The result is a CORS error in
 * the browser console that has nothing to do with CORS configuration
 * being wrong -- it is the redirect itself that should never have
 * been sent to a fetch caller.
 *
 * Registering this as the entry point for the whole filter chain
 * means every unauthenticated request to a protected endpoint gets a
 * clean 401 with a JSON body the frontend can branch on, and the
 * OAuth2 login flow remains reachable only through the explicit
 * /oauth2/authorization/google link that the frontend already uses.
 *
 * WHY NO JACKSON HERE
 *
 * The body has a small, fixed shape, so it is written directly as a
 * string rather than pulling in a JSON library dependency purely for
 * three fields. This keeps the class dependency-free and avoids
 * coupling it to whichever JSON library happens to be on the
 * classpath.
 */
@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String timestamp = LocalDateTime.now().toString();

        String body = "{"
                + "\"status\":401,"
                + "\"message\":\"Authentication is required for "
                + "this endpoint\","
                + "\"timestamp\":\"" + timestamp + "\""
                + "}";

        response.getWriter().write(body);
    }
}
