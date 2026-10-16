package com.ecom.products.search;

import com.ecom.products.models.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Full-text product search on Elasticsearch (HLD 3.2, PRD 2.3).
 * <p>
 * Everything here is best effort: indexing failures are logged, and {@link #search} returns empty when
 * Elasticsearch is unavailable so the caller can fall back to the database search.
 */
@Service
public class ProductSearchService {
    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ElasticsearchOperations operations;
    private final boolean enabled;
    private final int maxResults;
    private volatile boolean indexReady;

    public ProductSearchService(ElasticsearchOperations operations,
                                @Value("${search.elasticsearch.enabled:true}") boolean enabled,
                                @Value("${search.max-results:50}") int maxResults) {
        this.operations = operations;
        this.enabled = enabled;
        this.maxResults = maxResults;
    }

    /**
     * Ranked product ids matching the keyword, or empty if Elasticsearch is disabled or unreachable.
     * Matches typos ("phnoe" → phone; none for words under 4 letters) and word prefixes ("pho" → phone),
     * weighting name over category over
     * description.
     */
    public Optional<List<Long>> search(String keyword) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            ensureIndex();
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .should(s -> s.multiMatch(m -> m.query(keyword)
                                    .fields("name^3", "categoryName^2", "description")
                                    // No typos for words under 4 letters ("pho" must not match "pro"), 1 for 4-6, 2 for 7+
                                    .fuzziness("AUTO:4,7")))
                            .should(s -> s.multiMatch(m -> m.query(keyword)
                                    .fields("name^3", "categoryName^2")
                                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.PhrasePrefix)))
                            .minimumShouldMatch("1")))
                    .withPageable(PageRequest.of(0, maxResults))
                    .build();
            List<Long> ids = operations.search(query, ProductDocument.class).getSearchHits().stream()
                    .map(SearchHit::getId)
                    .map(Long::valueOf)
                    .toList();
            return Optional.of(ids);
        } catch (RuntimeException e) {
            log.warn("Elasticsearch search failed, falling back to database search: {}", e.getMessage());
            indexReady = false;
            return Optional.empty();
        }
    }

    public void index(Product product) {
        if (!enabled) {
            return;
        }
        try {
            ensureIndex();
            operations.save(toDocument(product));
        } catch (RuntimeException e) {
            log.warn("Could not index product {}: {}", product.getId(), e.getMessage());
            indexReady = false;
        }
    }

    public void remove(Long productId) {
        if (!enabled) {
            return;
        }
        try {
            ensureIndex();
            operations.delete(String.valueOf(productId), ProductDocument.class);
        } catch (RuntimeException e) {
            log.warn("Could not remove product {} from the search index: {}", productId, e.getMessage());
            indexReady = false;
        }
    }

    /**
     * Rebuilds the index from the given (non-deleted) products: drops it, recreates it with the mapping and
     * bulk-indexes everything, so products deleted while Elasticsearch was down disappear too.
     * @return number of products indexed
     */
    public int reindexAll(List<Product> products) {
        if (!enabled) {
            throw new IllegalStateException("Elasticsearch search is disabled (search.elasticsearch.enabled=false)");
        }
        IndexOperations indexOps = operations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
        indexReady = true;
        if (!products.isEmpty()) {
            operations.save(products.stream().map(ProductSearchService::toDocument).toList());
        }
        indexOps.refresh();
        return products.size();
    }

    private void ensureIndex() {
        if (indexReady) {
            return;
        }
        IndexOperations indexOps = operations.indexOps(IndexCoordinates.of("products"));
        if (!indexOps.exists()) {
            operations.indexOps(ProductDocument.class).createWithMapping();
        }
        indexReady = true;
    }

    private static ProductDocument toDocument(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(String.valueOf(product.getId()));
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setCategoryName(product.getCategory() == null ? null : product.getCategory().getName());
        doc.setPrice(product.getPrice());
        return doc;
    }
}
