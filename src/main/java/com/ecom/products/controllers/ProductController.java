package com.ecom.products.controllers;

import com.ecom.products.auth.AuthInterceptor;
import com.ecom.products.auth.AuthUserDto;
import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Product;
import com.ecom.products.services.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) throws ProductNotFoundException {
        Product product = productService.getProduct(id);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        return new ResponseEntity<>(productService.searchProducts(keyword), HttpStatus.OK);
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String categoryName) {
        return new ResponseEntity<>(productService.getProductsByCategory(categoryName), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody Product product,
                                              @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user) {
        // Record which user created the product
        product.setUserId(user.getId().intValue());
        Product newProduct = productService.addProduct(product);
        if (newProduct != null) {
            return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) throws ProductNotFoundException {
        boolean isDone = productService.deleteProduct(id);
        if (isDone) {
            return new ResponseEntity<>("Product deleted", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not deleted", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable Long id, @RequestBody Product product)
            throws ProductNotFoundException {
        product.setId(id);
        boolean isUpdated = productService.updateProduct(product);
        if (isUpdated) {
            return new ResponseEntity<>("Product updated", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not updated", HttpStatus.NOT_FOUND);
        }
    }
}
