package com.stormapi.collection.dto;

/**
 * Reusable key-value pair DTO for request headers.
 */
public record KeyValuePairDto(
        String key,
        String value
) {}
