package com.ecom.products.controllers;

import com.ecom.products.dtos.ProductNotFoundDto;
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

    @GetMapping("/")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) throws ProductNotFoundException {
        Product product = productService.getProduct(id);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }
    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        Product newProduct = productService.addProduct(product);
        if (newProduct != null) {
            return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/id")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) throws ProductNotFoundException {
        boolean isDone = productService.deleteProduct(id);
        if (isDone) {
            return new ResponseEntity<>("Product deleted", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not deleted", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/id")
    public ResponseEntity<String> updateProduct(@RequestBody Product product) throws ProductNotFoundException {
        boolean isUpdated = productService.updateProduct(product);
        if (isUpdated) {
            return new ResponseEntity<>("Product updated", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not updated", HttpStatus.NOT_FOUND);
        }
    }

   /* @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProductNotFoundDto> handleProductNotFound(ProductNotFoundException e) {
        ProductNotFoundDto productNotFoundDto = new ProductNotFoundDto();
        productNotFoundDto.setErrorCode(e.getErrorCode());
        productNotFoundDto.setErrorMessage(e.getMessage());
        return new ResponseEntity<>(productNotFoundDto, HttpStatus.NOT_FOUND);
    }*/
}
