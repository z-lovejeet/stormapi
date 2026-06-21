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
import com.stormapi.test.model.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CollectionMapper Unit Tests")
class CollectionMapperTest {

    @Test
    @DisplayName("toEntity maps fields from CreateCollectionRequest")
    void toEntity_mapsFields() {
        CreateCollectionRequest request = new CreateCollectionRequest("My APIs", "Test APIs");

        ApiCollection collection = CollectionMapper.toEntity(request);

        assertEquals("My APIs", collection.getName());
        assertEquals("Test APIs", collection.getDescription());
    }

    @Test
    @DisplayName("toResponse includes endpoints")
    void toResponse_includesEndpoints() {
        ApiCollection collection = buildCollection();
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .collection(collection).name("Login").url("https://api.example.com/login")
                .method(HttpMethod.POST).sortOrder(0).headers(new ArrayList<>()).build();
        endpoint.setId(1L);
        collection.getEndpoints().add(endpoint);

        CollectionResponse response = CollectionMapper.toResponse(collection);

        assertEquals(collection.getId(), response.id());
        assertEquals("Test Collection", response.name());
        assertEquals(1, response.endpoints().size());
        assertEquals("Login", response.endpoints().get(0).name());
    }

    @Test
    @DisplayName("toSummary includes endpoint count")
    void toSummary_includesCount() {
        ApiCollection collection = buildCollection();
        collection.getEndpoints().add(ApiEndpoint.builder().collection(collection).name("e1")
                .url("https://a.com").method(HttpMethod.GET).headers(new ArrayList<>()).build());
        collection.getEndpoints().add(ApiEndpoint.builder().collection(collection).name("e2")
                .url("https://b.com").method(HttpMethod.GET).headers(new ArrayList<>()).build());

        CollectionSummaryResponse summary = CollectionMapper.toSummary(collection);

        assertEquals(2, summary.endpointCount());
    }

    @Test
    @DisplayName("toEndpointEntity sets collection FK and maps headers")
    void toEndpointEntity_setsCollectionFK() {
        ApiCollection collection = buildCollection();
        CreateEndpointRequest request = new CreateEndpointRequest(
                "Login", "https://api.example.com/login", HttpMethod.POST,
                List.of(new KeyValuePairDto("Content-Type", "application/json")),
                "{\"user\":\"test\"}", "Login endpoint"
        );

        ApiEndpoint endpoint = CollectionMapper.toEndpointEntity(request, collection);

        assertEquals(collection, endpoint.getCollection());
        assertEquals("Login", endpoint.getName());
        assertEquals(HttpMethod.POST, endpoint.getMethod());
        assertEquals(1, endpoint.getHeaders().size());
        assertEquals("Content-Type", endpoint.getHeaders().get(0).getKey());
    }

    private ApiCollection buildCollection() {
        ApiCollection c = ApiCollection.builder()
                .name("Test Collection").description("desc")
                .endpoints(new ArrayList<>()).build();
        c.setId(1L);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

}
