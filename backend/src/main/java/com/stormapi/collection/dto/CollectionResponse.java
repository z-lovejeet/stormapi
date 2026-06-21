package com.stormapi.collection.dto;

import java.time.Instant;
import java.util.List;

/**
 * Full collection response with nested endpoints.
 */
public record CollectionResponse(
        Long id,
        String name,
        String description,
        List<EndpointResponse> endpoints,
        Instant createdAt,
        Instant updatedAt
) {}
