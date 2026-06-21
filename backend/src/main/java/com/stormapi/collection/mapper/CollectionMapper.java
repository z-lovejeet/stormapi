package com.stormapi.collection.mapper;

import com.stormapi.collection.dto.CollectionResponse;
import com.stormapi.collection.dto.CollectionSummaryResponse;
import com.stormapi.collection.dto.CreateCollectionRequest;
import com.stormapi.collection.dto.CreateEndpointRequest;
import com.stormapi.collection.dto.EndpointResponse;
import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.collection.model.ApiCollection;
import com.stormapi.collection.model.ApiEndpoint;
import com.stormapi.collection.model.KeyValuePair;

import java.util.List;

/**
 * Manual mapper for ApiCollection and ApiEndpoint entities.
 */
public final class CollectionMapper {

    private CollectionMapper() {}

    public static ApiCollection toEntity(CreateCollectionRequest request) {
        return ApiCollection.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    public static CollectionResponse toResponse(ApiCollection collection) {
        List<EndpointResponse> endpoints = collection.getEndpoints() != null
                ? collection.getEndpoints().stream()
                    .map(CollectionMapper::toEndpointResponse)
                    .toList()
                : List.of();

        return new CollectionResponse(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                endpoints,
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }

    public static CollectionSummaryResponse toSummary(ApiCollection collection) {
        int count = collection.getEndpoints() != null ? collection.getEndpoints().size() : 0;
        return new CollectionSummaryResponse(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                count,
                collection.getCreatedAt()
        );
    }

    public static ApiEndpoint toEndpointEntity(CreateEndpointRequest request, ApiCollection collection) {
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .collection(collection)
                .name(request.name())
                .url(request.url())
                .method(request.method())
                .body(request.body())
                .description(request.description())
                .sortOrder(collection.getEndpoints() != null ? collection.getEndpoints().size() : 0)
                .build();

        if (request.headers() != null) {
            endpoint.setHeaders(request.headers().stream()
                    .map(dto -> new KeyValuePair(dto.key(), dto.value()))
                    .toList());
        }

        return endpoint;
    }

    public static EndpointResponse toEndpointResponse(ApiEndpoint endpoint) {
        List<KeyValuePairDto> headers = endpoint.getHeaders() != null
                ? endpoint.getHeaders().stream()
                    .map(kv -> new KeyValuePairDto(kv.getKey(), kv.getValue()))
                    .toList()
                : List.of();

        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                endpoint.getMethod(),
                headers,
                endpoint.getBody(),
                endpoint.getDescription(),
                endpoint.getSortOrder()
        );
    }

}
