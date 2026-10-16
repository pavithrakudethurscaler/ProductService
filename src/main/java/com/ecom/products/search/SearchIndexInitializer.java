package com.ecom.products.search;

import com.ecom.products.repos.ProductRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Rebuilds the search index from the database on startup, in the background, so the index is correct even if
 * products changed while Elasticsearch was down. If Elasticsearch is unreachable this only logs a warning;
 * search falls back to the database and an admin can trigger a reindex later.
 */
@Component
public class SearchIndexInitializer {
    private static final Logger log = LoggerFactory.getLogger(SearchIndexInitializer.class);

    private final ProductSearchService searchService;
    private final ProductRepo productRepo;
    private final boolean enabled;

    public SearchIndexInitializer(ProductSearchService searchService, ProductRepo productRepo,
                                  @Value("${search.elasticsearch.enabled:true}") boolean enabled) {
        this.searchService = searchService;
        this.productRepo = productRepo;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reindexOnStartup() {
        if (!enabled) {
            return;
        }
        Thread.ofVirtual().name("search-reindex").start(() -> {
            try {
                int count = searchService.reindexAll(productRepo.findByDeletedFalse());
                log.info("Search index rebuilt with {} products", count);
            } catch (RuntimeException e) {
                log.warn("Search index not rebuilt on startup (Elasticsearch unavailable?): {}", e.getMessage());
            }
        });
    }
}
