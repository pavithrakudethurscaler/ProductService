package com.ecom.products.services;

import com.ecom.products.models.Product;

import java.util.List;

/**
 * Search results plus which engine produced them: "elasticsearch", or "database" when Elasticsearch is
 * disabled or unavailable (or for the FakeStore implementation).
 */
public record ProductSearchResult(List<Product> products, String engine) {
}
