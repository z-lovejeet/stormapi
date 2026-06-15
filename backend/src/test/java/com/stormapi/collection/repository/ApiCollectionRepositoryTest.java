package com.stormapi.collection.repository;

import com.stormapi.collection.model.ApiCollection;
import com.stormapi.collection.model.ApiEndpoint;
import com.stormapi.collection.model.KeyValuePair;
import com.stormapi.test.model.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ApiCollectionRepository and ApiEndpointRepository.
 * Verifies cascade persist, orphan removal, ElementCollection, and ordering.
 */
@DataJpaTest
@ActiveProfiles("test")
class ApiCollectionRepositoryTest {

    @Autowired
    private ApiCollectionRepository apiCollectionRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    private ApiCollection savedCollection;

    @BeforeEach
    void setUp() {
        apiCollectionRepository.deleteAll();

        savedCollection = ApiCollection.builder()
                .name("User Management API")
                .description("All user-related endpoints")
                .build();

        ApiEndpoint endpoint1 = ApiEndpoint.builder()
                .collection(savedCollection)
                .name("Get All Users")
                .url("https://api.example.com/users")
                .method(HttpMethod.GET)
                .sortOrder(0)
                .headers(new java.util.ArrayList<>(List.of(
                        new KeyValuePair("Accept", "application/json"),
                        new KeyValuePair("Authorization", "Bearer token")
                )))
                .build();

        ApiEndpoint endpoint2 = ApiEndpoint.builder()
                .collection(savedCollection)
                .name("Create User")
                .url("https://api.example.com/users")
                .method(HttpMethod.POST)
                .body("{\"name\": \"John\", \"email\": \"john@example.com\"}")
                .sortOrder(1)
                .build();

        savedCollection.getEndpoints().add(endpoint1);
        savedCollection.getEndpoints().add(endpoint2);

        savedCollection = apiCollectionRepository.save(savedCollection);
    }

    @Test
    void save_cascadePersistsEndpoints() {
        assertThat(apiEndpointRepository.findAll()).hasSize(2);
    }

    @Test
    void save_persistsElementCollectionHeaders() {
        List<ApiEndpoint> endpoints = apiEndpointRepository
                .findByCollectionIdOrderBySortOrderAsc(savedCollection.getId());

        ApiEndpoint getUsers = endpoints.get(0);
        assertThat(getUsers.getHeaders()).hasSize(2);
        assertThat(getUsers.getHeaders().get(0).getKey()).isEqualTo("Accept");
        assertThat(getUsers.getHeaders().get(1).getKey()).isEqualTo("Authorization");
    }

    @Test
    void findByCollectionIdOrderBySortOrderAsc_ordersCorrectly() {
        List<ApiEndpoint> endpoints = apiEndpointRepository
                .findByCollectionIdOrderBySortOrderAsc(savedCollection.getId());

        assertThat(endpoints).hasSize(2);
        assertThat(endpoints.get(0).getName()).isEqualTo("Get All Users");
        assertThat(endpoints.get(1).getName()).isEqualTo("Create User");
    }

    @Test
    void findAllByOrderByCreatedAtDesc_returnsCollections() {
        apiCollectionRepository.save(ApiCollection.builder()
                .name("Payment API")
                .build());

        List<ApiCollection> collections = apiCollectionRepository.findAllByOrderByCreatedAtDesc();

        assertThat(collections).hasSize(2);
        assertThat(collections.get(0).getName()).isEqualTo("Payment API");
    }

    @Test
    void orphanRemoval_deletesRemovedEndpoint() {
        // Re-fetch to get Hibernate-managed collection
        ApiCollection managed = apiCollectionRepository.findById(savedCollection.getId()).orElseThrow();

        // Remove one endpoint from the collection
        managed.getEndpoints().removeIf(e -> e.getName().equals("Create User"));
        apiCollectionRepository.saveAndFlush(managed);

        assertThat(apiEndpointRepository.findAll()).hasSize(1);
        assertThat(apiEndpointRepository.findAll().get(0).getName()).isEqualTo("Get All Users");
    }

    @Test
    void cascadeDelete_removesCollectionAndEndpoints() {
        apiCollectionRepository.delete(savedCollection);

        assertThat(apiCollectionRepository.findAll()).isEmpty();
        assertThat(apiEndpointRepository.findAll()).isEmpty();
    }

}
