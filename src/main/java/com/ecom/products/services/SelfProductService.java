package com.ecom.products.services;

import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Category;
import com.ecom.products.models.Product;
import com.ecom.products.repos.CategoryRepo;
import com.ecom.products.repos.ProductRepo;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Product service backed by this service's own database (HLD 3.2).
 * Marked @Primary so it is used instead of FakeProductService.
 */
@Primary
@Service
public class SelfProductService implements ProductService {
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;

    public SelfProductService(ProductRepo productRepo, CategoryRepo categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
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
        return productRepo.save(product);
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
        productRepo.save(existing);
        return true;
    }

    @Override
    public boolean deleteProduct(Long id) throws ProductNotFoundException {
        Product existing = getProduct(id);
        existing.setDeleted(true);
        productRepo.save(existing);
        return true;
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepo.findByNameContainingIgnoreCaseAndDeletedFalse(keyword);
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
