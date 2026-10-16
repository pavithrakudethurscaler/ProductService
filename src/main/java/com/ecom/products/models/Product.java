package com.ecom.products.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product extends BaseModel{
    private double price;
    private String description;
    private Category category;
}
