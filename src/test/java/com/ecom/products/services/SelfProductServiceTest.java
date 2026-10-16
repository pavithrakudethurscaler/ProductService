package com.ecom.products.services;

import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Category;
import com.ecom.products.models.Product;
import com.ecom.products.repos.CategoryRepo;
import com.ecom.products.repos.ProductRepo;
import com.ecom.products.search.ProductSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SelfProductService against real repositories (H2) with the search engine mocked. */
@DataJpaTest
class SelfProductServiceTest {
    @Autowired ProductRepo productRepo;
    @Autowired CategoryRepo categoryRepo;

    ProductSearchService search;
    SelfProductService service;

    @BeforeEach
    void setUp() {
        search = mock(ProductSearchService.class);
        service = new SelfProductService(productRepo, categoryRepo, search);
    }

    private Product product(String name, double price, String category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        Category c = new Category();
        c.setName(category);
        p.setCategory(c);
        return p;
    }

    @Test
    void addReusesCategoryIgnoringCaseAndIndexesProduct() {
        Product a = service.addProduct(product("Phone", 100, "Electronics"));
        Product b = service.addProduct(product("Laptop", 900, " electronics "));

        assertThat(b.getCategory().getId()).isEqualTo(a.getCategory().getId());
        assertThat(categoryRepo.count()).isEqualTo(1);
        verify(search).index(a);
        verify(search).index(b);
    }

    @Test
    void addIgnoresClientIdAndRequiresCategoryName() {
        Product existing = service.addProduct(product("Phone", 100, "E"));
        Product attempt = product("Other", 5, "E");
        attempt.setId(existing.getId());
        assertThat(service.addProduct(attempt).getId()).isNotEqualTo(existing.getId());

        assertThatThrownBy(() -> service.addProduct(product("X", 1, " ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void partialUpdateOnlyChangesProvidedFieldsAndPositivePrice() throws Exception {
        Product saved = service.addProduct(product("Phone", 100, "E"));
        Product patch = new Product();
        patch.setId(saved.getId());
        patch.setPrice(0); // not > 0: ignored
        patch.setDescription("5G");
        service.updateProduct(patch);

        Product reloaded = service.getProduct(saved.getId());
        assertThat(reloaded.getName()).isEqualTo("Phone");
        assertThat(reloaded.getPrice()).isEqualTo(100);
        assertThat(reloaded.getDescription()).isEqualTo("5G");

        patch.setPrice(90);
        service.updateProduct(patch);
        assertThat(service.getProduct(saved.getId()).getPrice()).isEqualTo(90);
    }

    @Test
    void softDeleteHidesProductAndRemovesItFromIndex() throws Exception {
        Product saved = service.addProduct(product("Phone", 100, "E"));
        service.deleteProduct(saved.getId());

        verify(search).remove(saved.getId());
        assertThat(productRepo.findById(saved.getId())).get().extracting(Product::isDeleted).isEqualTo(true);
        assertThatThrownBy(() -> service.getProduct(saved.getId())).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.deleteProduct(saved.getId())).isInstanceOf(ProductNotFoundException.class);
        assertThat(service.getAllProducts()).isEmpty();
    }

    @Test
    void searchKeepsElasticsearchRankingAndSkipsDeletedProducts() throws Exception {
        Product phone = service.addProduct(product("Smart Phone", 100, "E"));
        Product caseP = service.addProduct(product("Phone Case", 10, "A"));
        Product gone = service.addProduct(product("Old Phone", 5, "E"));
        service.deleteProduct(gone.getId());
        when(search.search("phone")).thenReturn(Optional.of(List.of(caseP.getId(), gone.getId(), phone.getId())));

        ProductSearchResult result = service.searchProducts(" phone ");

        assertThat(result.engine()).isEqualTo("elasticsearch");
        assertThat(result.products()).extracting(Product::getName).containsExactly("Phone Case", "Smart Phone");
    }

    @Test
    void searchFallsBackToDatabaseWhenElasticsearchUnavailable() {
        service.addProduct(product("Smart Phone", 100, "E"));
        service.addProduct(product("Laptop", 900, "E"));
        when(search.search(anyString())).thenReturn(Optional.empty());

        ProductSearchResult result = service.searchProducts("PHONE");

        assertThat(result.engine()).isEqualTo("database");
        assertThat(result.products()).extracting(Product::getName).containsExactly("Smart Phone");
    }

    @Test
    void blankKeywordIsRejected() {
        assertThatThrownBy(() -> service.searchProducts("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
