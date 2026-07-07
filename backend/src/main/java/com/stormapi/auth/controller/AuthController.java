package com.stormapi.auth.controller;

import com.stormapi.auth.dto.AuthRequest;
import com.stormapi.auth.dto.AuthStatusResponse;
import com.stormapi.auth.dto.RegisterRequest;
import com.stormapi.auth.dto.UserResponse;
import com.stormapi.auth.jwt.JwtTokenProvider;
import com.stormapi.auth.model.AppUser;
import com.stormapi.auth.model.AuthProvider;
import com.stormapi.auth.repository.AppUserRepository;
import com.stormapi.common.model.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints:
 * - POST /api/auth/register — local email/password registration
 * - POST /api/auth/login    — local email/password login
 * - GET  /api/auth/status   — public, returns current auth state
 * - GET  /api/auth/me       — protected, returns user profile
 * - POST /api/auth/logout   — protected, clears JWT cookie
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.email())) {
            return ApiResponse.error(400, "Bad Request", "Email is already registered", "EMAIL_EXISTS", "/api/auth/register");
        }

        AppUser user = AppUser.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .providerId(request.email())
                .build();

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getName());
        setAuthCookie(response, token);

        return ApiResponse.success(mapToUserResponse(user), "/api/auth/register");
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AppUser user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ApiResponse.error(401, "Unauthorized", "Invalid email or password", "INVALID_CREDENTIALS", "/api/auth/login");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getName());
        setAuthCookie(response, token);

        return ApiResponse.success(mapToUserResponse(user), "/api/auth/login");
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("stormapi_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    @GetMapping("/status")
    public ApiResponse<AuthStatusResponse> getAuthStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUser user) {
            UserResponse userResponse = mapToUserResponse(user);
            return ApiResponse.success(
                    AuthStatusResponse.builder()
                            .authenticated(true)
                            .user(userResponse)
                            .build(),
                    "/api/auth/status"
            );
        }

        return ApiResponse.success(
                AuthStatusResponse.builder()
                        .authenticated(false)
                        .build(),
                "/api/auth/status"
        );
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof AppUser user) {
            return ApiResponse.success(mapToUserResponse(user), "/api/auth/me");
        }

        return ApiResponse.error(401, "Unauthorized", "Not authenticated",
                "UNAUTHORIZED", "/api/auth/me");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        // Clear the JWT cookie
        Cookie cookie = new Cookie("stormapi_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete immediately
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        return ApiResponse.success(null, "/api/auth/logout");
    }

    private UserResponse mapToUserResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider().name())
                .build();
    }
}
