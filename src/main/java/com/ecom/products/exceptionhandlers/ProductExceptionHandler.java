package com.ecom.products.exceptionhandlers;

import com.ecom.products.dtos.ProductNotFoundDto;
import com.ecom.products.exceptions.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ProductExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProductNotFoundDto> handleProductNotFound(ProductNotFoundException e) {
        ProductNotFoundDto productNotFoundDto = new ProductNotFoundDto();
        productNotFoundDto.setErrorCode(e.getErrorCode());
        productNotFoundDto.setErrorMessage(e.getMessage());
        return new ResponseEntity<>(productNotFoundDto, HttpStatus.NOT_FOUND);
    }
}
