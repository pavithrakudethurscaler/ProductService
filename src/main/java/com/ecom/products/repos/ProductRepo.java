package com.ecom.products.repos;

import com.ecom.products.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findByDeletedFalse();

    Optional<Product> findByIdAndDeletedFalse(Long id);

    List<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword);

    List<Product> findByCategory_NameIgnoreCaseAndDeletedFalse(String categoryName);
}
