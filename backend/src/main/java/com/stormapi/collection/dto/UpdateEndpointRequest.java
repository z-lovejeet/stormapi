package com.stormapi.collection.dto;

import com.stormapi.common.validation.ValidUrl;
import com.stormapi.test.model.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating an endpoint.
 */
public record UpdateEndpointRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @ValidUrl String url,
        @NotNull HttpMethod method,
        List<KeyValuePairDto> headers,
        String body,
        @Size(max = 1000) String description
) {}
