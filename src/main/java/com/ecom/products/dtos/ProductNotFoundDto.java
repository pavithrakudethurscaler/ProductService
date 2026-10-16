package com.ecom.products.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductNotFoundDto {
    private int errorCode;
    private String errorMessage;
}
