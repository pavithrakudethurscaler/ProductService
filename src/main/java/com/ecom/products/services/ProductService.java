package com.ecom.products.services;

import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Category;
import com.ecom.products.models.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProduct(Long id) throws ProductNotFoundException;
    Product addProduct(Product product);
    boolean updateProduct(Product product) throws ProductNotFoundException;
    boolean deleteProduct(Long id) throws ProductNotFoundException;
    List<Product> searchProducts(String keyword);
    List<Product> getProductsByCategory(String categoryName);
    List<Category> getAllCategories();
    //Product replaceProduct(Long id, Product product);
}
