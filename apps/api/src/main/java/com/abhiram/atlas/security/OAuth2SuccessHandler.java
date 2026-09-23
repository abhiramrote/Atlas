package com.abhiram.atlas.security;

import com.abhiram.atlas.entity.AppUser;
import com.abhiram.atlas.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Completes OAuth2 login by issuing an Atlas token.
 *
 * The provider authenticates the person. Atlas then issues its own
 * token so the rest of the system never depends on provider tokens,
 * which have their own lifetimes and refresh semantics.
 *
 * The token is returned through a redirect fragment rather than a
 * query parameter, because query strings are written to server logs
 * and browser history while fragments are not sent to the server at
 * all.
 */
@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(
            UserService userService,
            JwtService jwtService,
            @Value("${atlas.frontend.url:http://localhost:5173}")
            String frontendUrl
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        AppUser user = userService.findOrCreate(
                "GOOGLE",
                oauthUser.getAttribute("sub"),
                oauthUser.getAttribute("email"),
                oauthUser.getAttribute("name"),
                oauthUser.getAttribute("picture")
        );

        String token = jwtService.issueToken(user);

        String target = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/auth/callback")
                .fragment("token=" + token)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(
                request,
                response,
                target
        );
    }
}
