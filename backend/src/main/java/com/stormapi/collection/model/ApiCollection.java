package com.stormapi.collection.model;

import com.stormapi.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Named group/folder of saved API endpoints — like a Postman collection.
 * Allows users to organize and reuse API endpoint configurations.
 */
@Entity
@Table(name = "api_collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCollection extends BaseEntity {

    /** Collection name */
    @Column(nullable = false, length = 255)
    private String name;

    /** Optional description */
    @Column(length = 1000)
    private String description;

    /** Endpoints belonging to this collection */
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApiEndpoint> endpoints = new ArrayList<>();

}
