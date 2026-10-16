package com.ecom.products.services;

import com.ecom.products.exceptions.ProductNotFoundException;
import com.ecom.products.models.Category;
import com.ecom.products.models.Product;
import com.ecom.products.dtos.FakeStoreProductDto;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeProductService implements ProductService {
    RestTemplate restTemplate;
    public FakeProductService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Override
    public List<Product> getAllProducts() {
        FakeStoreProductDto[] fakeStoreProductDtos = restTemplate.getForObject("https://fakestoreapi.com/products/", FakeStoreProductDto[].class);
        List<Product> products = new ArrayList<>();
        if (fakeStoreProductDtos != null) {
            for (FakeStoreProductDto fakeStoreProductDto : fakeStoreProductDtos) {
                products.add(convertDtoToProduct(fakeStoreProductDto));
            }
        }
        return products;
    }

    public Product getProduct(Long id) throws ProductNotFoundException {
        FakeStoreProductDto fakeStoreProductDto = restTemplate.getForObject("https://fakestoreapi.com/products/" + id, FakeStoreProductDto.class);
        if (fakeStoreProductDto == null) {
            throw new ProductNotFoundException(100, "Product not found:"+id);
        }
        return convertDtoToProduct(fakeStoreProductDto);
    }

    @Override
    public Product addProduct(Product product) {
        FakeStoreProductDto fakeStoreProductDto = new FakeStoreProductDto();
        fakeStoreProductDto.setTitle(product.getName());
        fakeStoreProductDto.setPrice(product.getPrice());
        fakeStoreProductDto.setCategory(product.getCategory().getName());
        fakeStoreProductDto.setDescription(product.getDescription());
        FakeStoreProductDto newProduct = restTemplate.postForObject("https://fakestoreapi.com/products", fakeStoreProductDto, FakeStoreProductDto.class);
        return (newProduct != null) ? convertDtoToProduct(newProduct) : null;
    }

    @Override
    public boolean updateProduct(Product product) throws ProductNotFoundException{
        FakeStoreProductDto fakeStoreProductDto = new FakeStoreProductDto();
        fakeStoreProductDto.setTitle(product.getName());
        fakeStoreProductDto.setPrice(product.getPrice());
        fakeStoreProductDto.setCategory(product.getCategory().getName());
        fakeStoreProductDto.setDescription(product.getDescription());
        try {
            restTemplate.put("https://fakestoreapi.com/products/" + product.getId(), fakeStoreProductDto);
        } catch (Exception e) {
            throw new ProductNotFoundException(100, "Product not found:"+product.getId());
        }
        return true;
    }

    /*@Override
    public Product replaceProduct(Long id, Product product) {
        FakeStoreProductDto fakeStoreProductDto = new FakeStoreProductDto();
        fakeStoreProductDto.setTitle(product.getName());
        fakeStoreProductDto.setDescription(product.getDescription());
        fakeStoreProductDto.setPrice(product.getPrice());

        RequestCallback requestCallback = restTemplate.httpEntityCallback(fakeStoreProductDto, FakeStoreProductDto.class);
        ResponseExtractor<ResponseEntity<FakeStoreProductDto>> responseExtractor =
                restTemplate.responseEntityExtractor(FakeStoreProductDto.class);

        FakeStoreProductDto fakeStoreProductDto1 =
                restTemplate
                        .execute("https://fakestoreapi.com/products/" + id, HttpMethod.PUT, requestCallback, responseExtractor)
                        .getBody();
        restTemplate.put("https://fakestoreapi.com/products/" + id, fakeStoreProductDto1);
        return convertDtoToProduct(fakeStoreProductDto1);
    }*/

    @Override
    public boolean deleteProduct(Long id) throws ProductNotFoundException {
        try {
            restTemplate.delete("https://fakestoreapi.com/products/" + id);
        } catch (Exception e) {
            throw new ProductNotFoundException(100, "Product not found:"+id);
        }
        return true;
    }

    private Product convertDtoToProduct(FakeStoreProductDto fakeStoreProductDto) {
        Product product = new Product();
        product.setId(fakeStoreProductDto.getId());
        product.setDescription(fakeStoreProductDto.getDescription());
        product.setPrice(fakeStoreProductDto.getPrice());
        product.setName(fakeStoreProductDto.getTitle());

        Category category = new Category();
        category.setName(fakeStoreProductDto.getCategory());
        product.setCategory(category);
        return product;

    }

}
