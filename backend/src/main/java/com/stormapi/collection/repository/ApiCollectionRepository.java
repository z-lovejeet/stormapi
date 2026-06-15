package com.stormapi.collection.repository;

import com.stormapi.collection.model.ApiCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ApiCollection entity.
 */
@Repository
public interface ApiCollectionRepository extends JpaRepository<ApiCollection, Long> {

    List<ApiCollection> findAllByOrderByCreatedAtDesc();

}
