package com.stormapi.config;

import com.stormapi.auth.handler.OAuth2AuthenticationFailureHandler;
import com.stormapi.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.stormapi.auth.jwt.JwtAuthenticationFilter;
import com.stormapi.auth.service.CustomOAuth2UserService;
import com.stormapi.auth.service.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security configuration:
 * - Stateless JWT via HttpOnly cookie
 * - OAuth2 login with Google + GitHub
 * - CSRF via double-submit cookie
 * - Public: landing page assets, auth status, actuator health, OAuth endpoints, WebSocket
 * - Protected: all /api/** endpoints
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                          OAuth2AuthenticationFailureHandler oAuth2FailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Use a simple request handler so the CSRF token is available as a request attribute
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        http
                // CSRF — double-submit cookie (readable by frontend JS, not HttpOnly)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler)
                        .ignoringRequestMatchers("/ws/**")  // WebSocket uses own lifecycle
                        .ignoringRequestMatchers("/api/auth/logout")  // Logout only clears a cookie
                )
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public — static assets (SPA)
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
                        // Public — auth status check & local auth
                        .requestMatchers("/api/auth/status", "/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
                        // Public — actuator health
                        .requestMatchers("/actuator/**").permitAll()
                        // Public — OAuth2 endpoints
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // Public — WebSocket
                        .requestMatchers("/ws/**").permitAll()
                        // Public — H2 console (dev only)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Public — Swagger/OpenAPI
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        // Protected — all API endpoints
                        .requestMatchers("/api/**").authenticated()
                        // Everything else — permit (SPA routing)
                        .anyRequest().permitAll()
                )
                // OAuth2 login
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                // JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Eagerly resolve CSRF token so cookie is set before first POST
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                // Stateless sessions — JWT handles auth state
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Custom 401 response for unauthenticated API requests
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"success\":false,\"error\":{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"errorCode\":\"UNAUTHORIZED\"},\"timestamp\":\"" +
                                    java.time.Instant.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}"
                            );
                        })
                )
                // Allow H2 console frames in dev
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}
