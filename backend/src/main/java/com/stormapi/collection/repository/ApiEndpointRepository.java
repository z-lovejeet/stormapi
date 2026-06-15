package com.stormapi.collection.repository;

import com.stormapi.collection.model.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ApiEndpoint entity.
 */
@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    List<ApiEndpoint> findByCollectionIdOrderBySortOrderAsc(Long collectionId);

}
