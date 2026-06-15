package com.stormapi.collection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reusable key-value pair for headers, query parameters, and form data.
 * Used as an @ElementCollection on ApiEndpoint.
 *
 * Column names use "pair_key" and "pair_value" because "key" and "value"
 * are SQL reserved words in some databases.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class KeyValuePair {

    @Column(name = "pair_key", nullable = false)
    private String key;

    @Column(name = "pair_value")
    private String value;

}
