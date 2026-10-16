package com.ecom.products.services;

import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Category;
import com.ecom.products.models.Product;
import com.ecom.products.repos.CategoryRepo;
import com.ecom.products.repos.ProductRepo;
import com.ecom.products.search.ProductSearchService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Product service backed by this service's own database (HLD 3.2).
 * Marked @Primary so it is used instead of FakeProductService.
 */
@Primary
@Service
public class SelfProductService implements ProductService {
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ProductSearchService searchService;

    public SelfProductService(ProductRepo productRepo, CategoryRepo categoryRepo, ProductSearchService searchService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.searchService = searchService;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepo.findByDeletedFalse();
    }

    @Override
    public Product getProduct(Long id) throws ProductNotFoundException {
        return productRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException(100, "Product not found:" + id));
    }

    @Override
    public Product addProduct(Product product) {
        product.setId(null);
        product.setCategory(resolveCategory(product.getCategory()));
        Product saved = productRepo.save(product);
        searchService.index(saved);
        return saved;
    }

    @Override
    public boolean updateProduct(Product product) throws ProductNotFoundException {
        Product existing = getProduct(product.getId());
        if (product.getName() != null) {
            existing.setName(product.getName());
        }
        if (product.getDescription() != null) {
            existing.setDescription(product.getDescription());
        }
        if (product.getPrice() > 0) {
            existing.setPrice(product.getPrice());
        }
        if (product.getCategory() != null) {
            existing.setCategory(resolveCategory(product.getCategory()));
        }
        searchService.index(productRepo.save(existing));
        return true;
    }

    @Override
    public boolean deleteProduct(Long id) throws ProductNotFoundException {
        Product existing = getProduct(id);
        existing.setDeleted(true);
        productRepo.save(existing);
        searchService.remove(id);
        return true;
    }

    /*
     1. Ask Elasticsearch for ranked matching ids (typo-tolerant, prefix-aware)
     2. Load those products from the database, keeping Elasticsearch's order and skipping deleted ones
        (the database is the source of truth; the index may briefly lag)
     3. If Elasticsearch is unavailable, fall back to the database name search
     */
    @Override
    public ProductSearchResult searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("keyword is required");
        }
        Optional<List<Long>> ids = searchService.search(keyword.trim());
        if (ids.isEmpty()) {
            return new ProductSearchResult(productRepo.findByNameContainingIgnoreCaseAndDeletedFalse(keyword.trim()),
                    "database");
        }
        Map<Long, Product> byId = productRepo.findAllById(ids.get()).stream()
                .filter(p -> !p.isDeleted())
                .collect(Collectors.toMap(Product::getId, p -> p));
        List<Product> ranked = ids.get().stream().map(byId::get).filter(Objects::nonNull).toList();
        return new ProductSearchResult(ranked, "elasticsearch");
    }

    /** Rebuilds the search index from the database (admin operation) */
    public int reindexSearch() {
        return searchService.reindexAll(productRepo.findByDeletedFalse());
    }

    @Override
    public List<Product> getProductsByCategory(String categoryName) {
        return productRepo.findByCategory_NameIgnoreCaseAndDeletedFalse(categoryName);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepo.findByDeletedFalse();
    }

    // Reuse an existing category with the same name, otherwise create it
    private Category resolveCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Product category name is required");
        }
        return categoryRepo.findByNameIgnoreCase(category.getName().trim())
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(category.getName().trim());
                    newCategory.setDescription(category.getDescription());
                    return categoryRepo.save(newCategory);
                });
    }
}
