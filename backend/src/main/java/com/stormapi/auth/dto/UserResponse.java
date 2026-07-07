package com.stormapi.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Public user info returned by auth endpoints.
 */
@Getter
@Builder
public class UserResponse {
    private final Long id;
    private final String email;
    private final String name;
    private final String avatarUrl;
    private final String provider;
}
