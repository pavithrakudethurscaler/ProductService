package com.ecom.products.services;

import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProduct(Long id) throws ProductNotFoundException;
    Product addProduct(Product product);
    boolean updateProduct(Product product) throws ProductNotFoundException;
    boolean deleteProduct(Long id) throws ProductNotFoundException;
    //Product replaceProduct(Long id, Product product);
}
