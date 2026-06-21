package com.stormapi.collection.dto;

import com.stormapi.test.model.HttpMethod;

import java.util.List;

/**
 * Response DTO for a single endpoint within a collection.
 */
public record EndpointResponse(
        Long id,
        String name,
        String url,
        HttpMethod method,
        List<KeyValuePairDto> headers,
        String body,
        String description,
        int sortOrder
) {}
