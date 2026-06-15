package com.stormapi.collection.model;

import com.stormapi.common.model.BaseEntity;
import com.stormapi.test.model.HttpMethod;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Saved API endpoint with full request configuration.
 * Like a Postman request — stores URL, method, headers, and body for reuse.
 */
@Entity
@Table(name = "api_endpoints", indexes = {
        @Index(name = "idx_api_endpoint_collection_id", columnList = "collection_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiEndpoint extends BaseEntity {

    /** The collection this endpoint belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private ApiCollection collection;

    /** Endpoint display name */
    @Column(nullable = false, length = 255)
    private String name;

    /** Target URL */
    @Column(nullable = false, length = 2048)
    private String url;

    /** HTTP method */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod method;

    /** Request headers as key-value pairs */
    @ElementCollection
    @CollectionTable(
            name = "endpoint_headers",
            joinColumns = @JoinColumn(name = "endpoint_id")
    )
    @Builder.Default
    private List<KeyValuePair> headers = new ArrayList<>();

    /** Request body (JSON, XML, etc.) */
    @Column(columnDefinition = "TEXT")
    private String body;

    /** Optional description */
    @Column(length = 1000)
    private String description;

    /** Display order within the collection */
    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;

}
