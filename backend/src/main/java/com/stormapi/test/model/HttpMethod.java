package com.stormapi.test.model;

/**
 * HTTP methods supported for API testing.
 * Custom enum decoupled from Spring's HttpMethod to keep the domain layer framework-agnostic.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS
}
