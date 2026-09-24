package com.abhiram.atlas.config;

import com.abhiram.atlas.domain.UserRole;
import com.abhiram.atlas.security.JwtAuthenticationFilter;
import com.abhiram.atlas.security.OAuth2SuccessHandler;
import com.abhiram.atlas.security.RestAuthenticationEntryPoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


/**
 * Atlas security rules.
 *
 * ACCESS MODEL
 *
 * Research data is public. Rankings, company pages, scores and data
 * quality reports can be read without authentication, because the
 * value of Atlas is in the analysis being inspectable and hiding it
 * behind a login adds friction without protecting anything.
 *
 * Theses require authentication. A research record only means
 * something if it belongs to a specific person, so writing one
 * demands an identity.
 *
 * Admin endpoints require elevation. A refresh call consumes rate
 * limited provider quota and mutates data every reader depends on.
 * Leaving it open lets any visitor exhaust the API budget.
 *
 * STATELESS BY DESIGN
 *
 * Sessions are disabled. Atlas runs the frontend and API separately,
 * and server side sessions would require sticky routing or shared
 * session storage to survive more than one instance.
 *
 * CSRF is disabled for the same reason. CSRF protects cookie based
 * authentication; a bearer token in a header is not sent
 * automatically by the browser, so the attack does not apply.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final OAuth2SuccessHandler successHandler;
    private final String frontendUrl;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

public SecurityConfig(
        JwtAuthenticationFilter jwtFilter,
        OAuth2SuccessHandler successHandler,
        RestAuthenticationEntryPoint restAuthenticationEntryPoint,
        @Value("${atlas.frontend.url:http://localhost:5173}")
        String frontendUrl
) {
    this.jwtFilter = jwtFilter;
    this.successHandler = successHandler;
    this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    this.frontendUrl = frontendUrl;
}


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(restAuthenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth

                        // Public research surface.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/companies/**",
                                "/api/instruments/**",
                                "/api/opportunities/**",
                                "/api/growth/**",
                                "/api/risk/**",
                                "/api/momentum/**",
                                "/api/prices/**",
                                "/api/sectors/**",
                                "/api/data-quality/**",
                                "/api/research/**"
                        ).permitAll()

                        // Login and health.
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**",
                                "/api/auth/**",
                                "/actuator/health"
                        ).permitAll()

                        // Operations that consume provider quota
                        // or mutate shared data.
                        .requestMatchers("/api/admin/**")
                        .hasRole(UserRole.ADMIN.name())

                        // Research records need an owner.
                        .requestMatchers("/api/theses/**")
                        .authenticated()

                        .anyRequest().authenticated()
                        
                )

                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler))

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS is restricted to the configured frontend origin.
     *
     * A wildcard would let any site call the API with a token the
     * browser happens to hold, which defeats the purpose of having
     * authentication at all.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(frontendUrl));

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }
}
