package com.stormapi.auth.controller;

import com.stormapi.auth.dto.AuthStatusResponse;
import com.stormapi.auth.dto.UserResponse;
import com.stormapi.auth.model.AppUser;
import com.stormapi.common.model.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints:
 * - GET  /api/auth/status — public, returns current auth state
 * - GET  /api/auth/me     — protected, returns user profile
 * - POST /api/auth/logout — protected, clears JWT cookie
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
        response.addCookie(cookie);

        return ApiResponse.success("/api/auth/logout");
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
