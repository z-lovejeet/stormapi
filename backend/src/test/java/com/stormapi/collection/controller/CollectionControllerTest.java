package com.stormapi.collection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stormapi.collection.dto.CreateCollectionRequest;
import com.stormapi.collection.dto.CreateEndpointRequest;
import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.collection.dto.UpdateCollectionRequest;
import com.stormapi.collection.model.ApiCollection;
import com.stormapi.collection.model.ApiEndpoint;
import com.stormapi.collection.service.CollectionService;
import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.test.model.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CollectionController.class)
@DisplayName("CollectionController WebMvc Tests")
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectionService collectionService;

    @Test
    @DisplayName("POST /api/collections returns 201")
    void createCollection_returns201() throws Exception {
        CreateCollectionRequest request = new CreateCollectionRequest("My APIs", "desc");
        when(collectionService.create(any())).thenReturn(buildCollection(1L));

        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("My APIs"));
    }

    @Test
    @DisplayName("POST /api/collections with blank name returns 400")
    void createCollection_blankName_returns400() throws Exception {
        String body = """
                { "name": "", "description": "desc" }
                """;

        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/collections returns list")
    void listCollections_returnsList() throws Exception {
        when(collectionService.listAll()).thenReturn(List.of(buildCollection(1L)));

        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/collections/{id} returns collection with endpoints")
    void getCollection_returnsWithEndpoints() throws Exception {
        when(collectionService.getById(1L)).thenReturn(buildCollection(1L));

        mockMvc.perform(get("/api/collections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.endpoints").isArray());
    }

    @Test
    @DisplayName("GET /api/collections/{id} not found returns 404")
    void getCollection_notFound_returns404() throws Exception {
        when(collectionService.getById(99L))
                .thenThrow(new ResourceNotFoundException("ApiCollection", 99L));

        mockMvc.perform(get("/api/collections/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/collections/{id} updates and returns 200")
    void updateCollection_returns200() throws Exception {
        UpdateCollectionRequest request = new UpdateCollectionRequest("Updated", "new desc");
        ApiCollection updated = buildCollection(1L);
        updated.setName("Updated");
        when(collectionService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/collections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /api/collections/{id} returns 204")
    void deleteCollection_returns204() throws Exception {
        mockMvc.perform(delete("/api/collections/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/collections/{id}/endpoints adds endpoint and returns 201")
    void addEndpoint_returns201() throws Exception {
        CreateEndpointRequest request = new CreateEndpointRequest(
                "Login", "https://api.example.com/login", HttpMethod.POST,
                List.of(new KeyValuePairDto("Content-Type", "application/json")),
                "{}", "Login endpoint"
        );
        ApiEndpoint endpoint = ApiEndpoint.builder()
                .collection(buildCollection(1L)).name("Login")
                .url("https://api.example.com/login").method(HttpMethod.POST)
                .sortOrder(0).headers(new ArrayList<>()).build();
        endpoint.setId(10L);
        when(collectionService.addEndpoint(eq(1L), any())).thenReturn(endpoint);

        mockMvc.perform(post("/api/collections/1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Login"));
    }

    @Test
    @DisplayName("DELETE /api/collections/{id}/endpoints/{eid} returns 204")
    void deleteEndpoint_returns204() throws Exception {
        mockMvc.perform(delete("/api/collections/1/endpoints/10"))
                .andExpect(status().isNoContent());
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
