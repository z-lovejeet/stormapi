package com.stormapi.collection.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request to reorder endpoints within a collection.
 * Contains the endpoint IDs in their new desired order.
 */
public record ReorderEndpointsRequest(
        @NotEmpty(message = "Endpoint IDs list must not be empty")
        List<Long> endpointIds
) {
}
