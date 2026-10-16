package com.ecom.products.controllers;

import com.ecom.products.services.SelfProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Search index maintenance. POST under /api/v1/products, so AuthInterceptor requires the ADMIN role. */
@RestController
@RequestMapping("/api/v1/products/search")
public class SearchAdminController {
    SelfProductService selfProductService;

    SearchAdminController(SelfProductService selfProductService) {
        this.selfProductService = selfProductService;
    }

    /** Rebuilds the Elasticsearch index from the database, e.g. after Elasticsearch was down */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        try {
            int count = selfProductService.reindexSearch();
            return new ResponseEntity<>(Map.of("indexed", count), HttpStatus.OK);
        } catch (RuntimeException e) {
            // Elasticsearch unreachable or disabled; product data itself is unaffected
            return new ResponseEntity<>(Map.of("errorMessage", "Search engine unavailable: " + e.getMessage()),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
