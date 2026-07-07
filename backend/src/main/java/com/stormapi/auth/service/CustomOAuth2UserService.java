package com.stormapi.auth.service;

import com.stormapi.auth.model.AppUser;
import com.stormapi.auth.model.AuthProvider;
import com.stormapi.auth.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Custom OAuth2 user service that creates or updates AppUser in the database
 * when a user authenticates via Google or GitHub.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final AppUserRepository appUserRepository;

    public CustomOAuth2UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = extractProviderId(attributes, provider);
        String email = extractEmail(attributes, provider);
        String name = extractName(attributes, provider);
        String avatarUrl = extractAvatarUrl(attributes, provider);

        AppUser user = appUserRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    // Update profile on each login
                    existingUser.setName(name);
                    existingUser.setAvatarUrl(avatarUrl);
                    if (email != null) {
                        existingUser.setEmail(email);
                    }
                    return appUserRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    log.info("Creating new user: {} ({})", email, provider);
                    AppUser newUser = AppUser.builder()
                            .email(email != null ? email : providerId + "@" + provider.name().toLowerCase())
                            .name(name)
                            .avatarUrl(avatarUrl)
                            .provider(provider)
                            .providerId(providerId)
                            .build();
                    return appUserRepository.save(newUser);
                });

        log.debug("OAuth2 user loaded: {} (id={})", user.getEmail(), user.getId());
        return oAuth2User;
    }

    private String extractProviderId(Map<String, Object> attributes, AuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("sub");
            case GITHUB -> String.valueOf(attributes.get("id"));
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    private String extractEmail(Map<String, Object> attributes, AuthProvider provider) {
        return (String) attributes.get("email");
    }

    private String extractName(Map<String, Object> attributes, AuthProvider provider) {
        String name = (String) attributes.get("name");
        if (name != null) return name;
        // GitHub fallback
        String login = (String) attributes.get("login");
        return login != null ? login : "User";
    }

    private String extractAvatarUrl(Map<String, Object> attributes, AuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("picture");
            case GITHUB -> (String) attributes.get("avatar_url");
            default -> null;
        };
    }
}
