package com.ecom.products.exceptionhandlers;

import com.ecom.common.dtos.ErrorDto;
import com.ecom.products.auth.AuthServiceUnavailableException;
import com.ecom.products.auth.UnauthorizedException;
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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDto> handleUnauthorized(UnauthorizedException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthServiceUnavailableException.class)
    public ResponseEntity<ErrorDto> handleAuthServiceUnavailable(AuthServiceUnavailableException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
