package com.stormapi.collection.dto;

import java.time.Instant;

/**
 * Lightweight collection response for list views — includes endpoint count instead of full list.
 */
public record CollectionSummaryResponse(
        Long id,
        String name,
        String description,
        int endpointCount,
        Instant createdAt
) {}
