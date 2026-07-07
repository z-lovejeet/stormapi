package com.stormapi.auth.handler;

import com.stormapi.auth.jwt.JwtTokenProvider;
import com.stormapi.auth.model.AppUser;
import com.stormapi.auth.model.AuthProvider;
import com.stormapi.auth.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * On successful OAuth2 login:
 * 1. Looks up the AppUser by provider + providerId
 * 2. Generates a JWT token
 * 3. Sets the JWT in a Secure HttpOnly cookie
 * 4. Redirects to the frontend dashboard
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private static final String COOKIE_NAME = "stormapi_token";

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository appUserRepository;
    private final boolean cookieSecure;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            AppUserRepository appUserRepository,
            @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.auth.redirect-uri:http://localhost:5173/dashboard}") String redirectUri) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appUserRepository = appUserRepository;
        this.cookieSecure = cookieSecure;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();
        String registrationId = authToken.getAuthorizedClientRegistrationId();

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId = extractProviderId(attributes, provider);

        AppUser user = appUserRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found after OAuth2 login — CustomOAuth2UserService should have created it"));

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getName());

        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        cookie.setAttribute("SameSite", "Lax");

        response.addCookie(cookie);

        log.info("OAuth2 login success for user: {} ({}). Redirecting to {}",
                user.getEmail(), provider, redirectUri);

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String extractProviderId(Map<String, Object> attributes, AuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("sub");
            case GITHUB -> String.valueOf(attributes.get("id"));
        };
    }
}
