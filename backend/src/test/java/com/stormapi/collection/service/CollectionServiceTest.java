package com.stormapi.collection.service;

import com.stormapi.collection.dto.CreateCollectionRequest;
import com.stormapi.collection.dto.CreateEndpointRequest;
import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.collection.dto.UpdateCollectionRequest;
import com.stormapi.collection.model.ApiCollection;
import com.stormapi.collection.model.ApiEndpoint;
import com.stormapi.collection.repository.ApiCollectionRepository;
import com.stormapi.collection.repository.ApiEndpointRepository;
import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.test.model.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionService Unit Tests")
class CollectionServiceTest {

    @Mock
    private ApiCollectionRepository collectionRepository;

    @Mock
    private ApiEndpointRepository endpointRepository;

    @InjectMocks
    private CollectionService collectionService;

    @Test
    @DisplayName("create saves and returns collection")
    void create_savesAndReturns() {
        CreateCollectionRequest request = new CreateCollectionRequest("My APIs", "desc");
        ApiCollection saved = buildCollection(1L);
        when(collectionRepository.save(any())).thenReturn(saved);

        ApiCollection result = collectionService.create(request);
        assertEquals(1L, result.getId());
        assertEquals("My APIs", result.getName());
    }

    @Test
    @DisplayName("getById returns collection when exists")
    void getById_exists() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(buildCollection(1L)));
        ApiCollection result = collectionService.getById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getById throws when not found")
    void getById_notFound_throws() {
        when(collectionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> collectionService.getById(99L));
    }

    @Test
    @DisplayName("update modifies name and description")
    void update_modifiesFields() {
        ApiCollection existing = buildCollection(1L);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateCollectionRequest request = new UpdateCollectionRequest("Updated", "new desc");
        ApiCollection result = collectionService.update(1L, request);

        assertEquals("Updated", result.getName());
        assertEquals("new desc", result.getDescription());
    }

    @Test
    @DisplayName("delete throws when not found")
    void delete_notFound_throws() {
        when(collectionRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> collectionService.delete(99L));
    }

    @Test
    @DisplayName("delete succeeds when exists")
    void delete_exists_succeeds() {
        when(collectionRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> collectionService.delete(1L));
        verify(collectionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("addEndpoint saves endpoint with collection FK")
    void addEndpoint_savesWithFK() {
        ApiCollection collection = buildCollection(1L);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        when(endpointRepository.save(any())).thenAnswer(i -> {
            ApiEndpoint e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        CreateEndpointRequest request = new CreateEndpointRequest(
                "Login", "https://api.example.com/login", HttpMethod.POST,
                List.of(new KeyValuePairDto("Content-Type", "application/json")),
                "{}", "Login endpoint"
        );

        ApiEndpoint result = collectionService.addEndpoint(1L, request);
        assertEquals(10L, result.getId());
        assertEquals(collection, result.getCollection());
    }

    @Test
    @DisplayName("deleteEndpoint validates ownership")
    void deleteEndpoint_wrongCollection_throws() {
        ApiCollection wrongCollection = buildCollection(2L);
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .collection(wrongCollection).name("e").url("https://a.com")
                .method(HttpMethod.GET).headers(new ArrayList<>()).build();
        endpoint.setId(10L);

        when(collectionRepository.existsById(1L)).thenReturn(true);
        when(endpointRepository.findById(10L)).thenReturn(Optional.of(endpoint));

        assertThrows(ResourceNotFoundException.class,
                () -> collectionService.deleteEndpoint(1L, 10L));
    }

    private ApiCollection buildCollection(Long id) {
        ApiCollection c = ApiCollection.builder()
                .name("My APIs").description("desc")
                .endpoints(new ArrayList<>()).build();
        c.setId(id);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

}
