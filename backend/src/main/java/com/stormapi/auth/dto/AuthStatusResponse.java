package com.stormapi.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Auth status response — indicates if the current request is authenticated
 * and includes user details if so.
 */
@Getter
@Builder
public class AuthStatusResponse {
    private final boolean authenticated;
    private final UserResponse user;
}
