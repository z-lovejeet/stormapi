package com.stormapi.auth.service;

import com.stormapi.auth.model.AppUser;
import com.stormapi.auth.model.AuthProvider;
import com.stormapi.auth.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Custom OIDC user service that creates or updates AppUser in the database
 * when a user authenticates via OIDC providers (e.g. Google).
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final AppUserRepository appUserRepository;

    public CustomOidcUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oidcUser.getAttributes();
        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String avatarUrl = oidcUser.getPicture();

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

        log.debug("OIDC user loaded: {} (id={})", user.getEmail(), user.getId());
        return oidcUser;
    }
}
