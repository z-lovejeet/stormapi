package com.stormapi.collection.controller;

import com.stormapi.collection.dto.CollectionResponse;
import com.stormapi.collection.dto.CollectionSummaryResponse;
import com.stormapi.collection.dto.CreateCollectionRequest;
import com.stormapi.collection.dto.CreateEndpointRequest;
import com.stormapi.collection.dto.EndpointResponse;
import com.stormapi.collection.dto.UpdateCollectionRequest;
import com.stormapi.collection.dto.UpdateEndpointRequest;
import com.stormapi.collection.mapper.CollectionMapper;
import com.stormapi.collection.model.ApiCollection;
import com.stormapi.collection.model.ApiEndpoint;
import com.stormapi.collection.service.CollectionService;
import com.stormapi.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for API collection and endpoint management.
 */
@RestController
@RequestMapping("/api/collections")
@Tag(name = "Collections", description = "API collection and endpoint management")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping
    @Operation(summary = "Create a new API collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> createCollection(
            @Valid @RequestBody CreateCollectionRequest request,
            HttpServletRequest httpRequest) {

        ApiCollection collection = collectionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        CollectionMapper.toResponse(collection), httpRequest.getRequestURI()));
    }

    @GetMapping
    @Operation(summary = "List all collections with endpoint counts")
    public ResponseEntity<ApiResponse<List<CollectionSummaryResponse>>> listCollections(
            HttpServletRequest httpRequest) {

        List<CollectionSummaryResponse> summaries = collectionService.listAll().stream()
                .map(CollectionMapper::toSummary)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(summaries, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection with all endpoints")
    public ResponseEntity<ApiResponse<CollectionResponse>> getCollection(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        ApiCollection collection = collectionService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(
                CollectionMapper.toResponse(collection), httpRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update collection name and description")
    public ResponseEntity<ApiResponse<CollectionResponse>> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollectionRequest request,
            HttpServletRequest httpRequest) {

        ApiCollection collection = collectionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                CollectionMapper.toResponse(collection), httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete collection and all endpoints")
    public ResponseEntity<ApiResponse<Void>> deleteCollection(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        collectionService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(httpRequest.getRequestURI()));
    }

    // ── Endpoint Operations ──

    @PostMapping("/{collectionId}/endpoints")
    @Operation(summary = "Add an endpoint to a collection")
    public ResponseEntity<ApiResponse<EndpointResponse>> addEndpoint(
            @PathVariable Long collectionId,
            @Valid @RequestBody CreateEndpointRequest request,
            HttpServletRequest httpRequest) {

        ApiEndpoint endpoint = collectionService.addEndpoint(collectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        CollectionMapper.toEndpointResponse(endpoint), httpRequest.getRequestURI()));
    }

    @PutMapping("/{collectionId}/endpoints/{endpointId}")
    @Operation(summary = "Update an endpoint")
    public ResponseEntity<ApiResponse<EndpointResponse>> updateEndpoint(
            @PathVariable Long collectionId,
            @PathVariable Long endpointId,
            @Valid @RequestBody UpdateEndpointRequest request,
            HttpServletRequest httpRequest) {

        ApiEndpoint endpoint = collectionService.updateEndpoint(collectionId, endpointId, request);
        return ResponseEntity.ok(ApiResponse.success(
                CollectionMapper.toEndpointResponse(endpoint), httpRequest.getRequestURI()));
    }

    @DeleteMapping("/{collectionId}/endpoints/{endpointId}")
    @Operation(summary = "Remove an endpoint from a collection")
    public ResponseEntity<ApiResponse<Void>> deleteEndpoint(
            @PathVariable Long collectionId,
            @PathVariable Long endpointId,
            HttpServletRequest httpRequest) {

        collectionService.deleteEndpoint(collectionId, endpointId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(httpRequest.getRequestURI()));
    }

    @PutMapping("/{collectionId}/endpoints/reorder")
    @Operation(summary = "Reorder endpoints within a collection")
    public ResponseEntity<ApiResponse<List<EndpointResponse>>> reorderEndpoints(
            @PathVariable Long collectionId,
            @Valid @RequestBody com.stormapi.collection.dto.ReorderEndpointsRequest request,
            HttpServletRequest httpRequest) {

        List<com.stormapi.collection.model.ApiEndpoint> reordered =
                collectionService.reorderEndpoints(collectionId, request.endpointIds());
        List<EndpointResponse> responses = reordered.stream()
                .map(CollectionMapper::toEndpointResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, httpRequest.getRequestURI()));
    }

    @PostMapping("/{collectionId}/endpoints/{endpointId}/duplicate")
    @Operation(summary = "Duplicate an endpoint within the same collection")
    public ResponseEntity<ApiResponse<EndpointResponse>> duplicateEndpoint(
            @PathVariable Long collectionId,
            @PathVariable Long endpointId,
            HttpServletRequest httpRequest) {

        ApiEndpoint copy = collectionService.duplicateEndpoint(collectionId, endpointId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        CollectionMapper.toEndpointResponse(copy), httpRequest.getRequestURI()));
    }

}
