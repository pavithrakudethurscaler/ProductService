package com.ecom.products.search;

import com.ecom.products.models.Product;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductSearchServiceTest {
    private final ElasticsearchOperations operations = mock(ElasticsearchOperations.class);

    @Test
    void disabledSearchNeverTouchesElasticsearch() {
        ProductSearchService service = new ProductSearchService(operations, false, 50);
        Product p = new Product();
        p.setId(1L);

        assertThat(service.search("phone")).isEmpty();
        service.index(p);
        service.remove(1L);
        assertThatThrownBy(() -> service.reindexAll(List.of())).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(operations);
    }

    @Test
    void unreachableElasticsearchMeansEmptyResultSoCallerFallsBack() {
        when(operations.indexOps(any(org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        ProductSearchService service = new ProductSearchService(operations, true, 50);

        assertThat(service.search("phone")).isEmpty();
    }
}
