package com.stormapi.collection.service;

import com.stormapi.collection.dto.CreateCollectionRequest;
import com.stormapi.collection.dto.CreateEndpointRequest;
import com.stormapi.collection.dto.UpdateCollectionRequest;
import com.stormapi.collection.dto.UpdateEndpointRequest;
import com.stormapi.collection.mapper.CollectionMapper;
import com.stormapi.collection.model.ApiCollection;
import com.stormapi.collection.model.ApiEndpoint;
import com.stormapi.collection.model.KeyValuePair;
import com.stormapi.collection.repository.ApiCollectionRepository;
import com.stormapi.collection.repository.ApiEndpointRepository;
import com.stormapi.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for API collection and endpoint CRUD operations.
 */
@Service
@Transactional
public class CollectionService {

    private final ApiCollectionRepository collectionRepository;
    private final ApiEndpointRepository endpointRepository;

    public CollectionService(ApiCollectionRepository collectionRepository,
                             ApiEndpointRepository endpointRepository) {
        this.collectionRepository = collectionRepository;
        this.endpointRepository = endpointRepository;
    }

    public ApiCollection create(CreateCollectionRequest request) {
        ApiCollection collection = CollectionMapper.toEntity(request);
        return collectionRepository.save(collection);
    }

    @Transactional(readOnly = true)
    public List<ApiCollection> listAll() {
        return collectionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public ApiCollection getById(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiCollection", id));
    }

    public ApiCollection update(Long id, UpdateCollectionRequest request) {
        ApiCollection collection = getById(id);
        collection.setName(request.name());
        collection.setDescription(request.description());
        return collectionRepository.save(collection);
    }

    public void delete(Long id) {
        if (!collectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("ApiCollection", id);
        }
        collectionRepository.deleteById(id);
    }

    // ── Endpoint Operations ──

    public ApiEndpoint addEndpoint(Long collectionId, CreateEndpointRequest request) {
        ApiCollection collection = getById(collectionId);
        ApiEndpoint endpoint = CollectionMapper.toEndpointEntity(request, collection);
        return endpointRepository.save(endpoint);
    }

    public ApiEndpoint updateEndpoint(Long collectionId, Long endpointId, UpdateEndpointRequest request) {
        ApiEndpoint endpoint = getEndpoint(collectionId, endpointId);
        endpoint.setName(request.name());
        endpoint.setUrl(request.url());
        endpoint.setMethod(request.method());
        endpoint.setBody(request.body());
        endpoint.setDescription(request.description());
        if (request.headers() != null) {
            endpoint.setHeaders(request.headers().stream()
                    .map(dto -> new KeyValuePair(dto.key(), dto.value()))
                    .toList());
        }
        return endpointRepository.save(endpoint);
    }

    public void deleteEndpoint(Long collectionId, Long endpointId) {
        ApiEndpoint endpoint = getEndpoint(collectionId, endpointId);
        endpointRepository.delete(endpoint);
    }

    /**
     * Reorder endpoints within a collection. Updates sortOrder to match
     * the order of IDs in the request.
     */
    public List<ApiEndpoint> reorderEndpoints(Long collectionId, List<Long> endpointIds) {
        // Verify collection exists
        if (!collectionRepository.existsById(collectionId)) {
            throw new ResourceNotFoundException("ApiCollection", collectionId);
        }
        List<ApiEndpoint> endpoints = endpointRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);

        // Validate all IDs belong to this collection
        var existingIds = endpoints.stream().map(ApiEndpoint::getId).collect(java.util.stream.Collectors.toSet());
        for (Long eid : endpointIds) {
            if (!existingIds.contains(eid)) {
                throw new ResourceNotFoundException("ApiEndpoint " + eid
                        + " does not belong to collection " + collectionId);
            }
        }

        // Build lookup and update sort order
        var endpointMap = endpoints.stream()
                .collect(java.util.stream.Collectors.toMap(ApiEndpoint::getId, e -> e));
        List<ApiEndpoint> reordered = new java.util.ArrayList<>();
        for (int i = 0; i < endpointIds.size(); i++) {
            ApiEndpoint ep = endpointMap.get(endpointIds.get(i));
            if (ep != null) {
                ep.setSortOrder(i);
                reordered.add(ep);
            }
        }
        return endpointRepository.saveAll(reordered);
    }

    /**
     * Duplicate an endpoint within the same collection.
     */
    public ApiEndpoint duplicateEndpoint(Long collectionId, Long endpointId) {
        ApiEndpoint source = getEndpoint(collectionId, endpointId);
        ApiEndpoint copy = ApiEndpoint.builder()
                .collection(source.getCollection())
                .name(source.getName() + " (Copy)")
                .url(source.getUrl())
                .method(source.getMethod())
                .headers(new java.util.ArrayList<>(source.getHeaders()))
                .body(source.getBody())
                .description(source.getDescription())
                .sortOrder(source.getSortOrder() + 1)
                .build();
        return endpointRepository.save(copy);
    }

    private ApiEndpoint getEndpoint(Long collectionId, Long endpointId) {
        // Verify collection exists
        if (!collectionRepository.existsById(collectionId)) {
            throw new ResourceNotFoundException("ApiCollection", collectionId);
        }
        ApiEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiEndpoint", endpointId));
        // Verify ownership
        if (!endpoint.getCollection().getId().equals(collectionId)) {
            throw new ResourceNotFoundException("ApiEndpoint " + endpointId
                    + " does not belong to collection " + collectionId);
        }
        return endpoint;
    }

}
