package com.ecom.products.controllers;

import com.ecom.products.auth.AuthService;
import com.ecom.products.auth.AuthUserDto;
import com.ecom.products.auth.UnauthorizedException;
import com.ecom.products.models.Product;
import com.ecom.products.services.ProductSearchResult;
import com.ecom.products.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP behaviour: public reads, X-Search-Engine header, and the AuthInterceptor's 401/403/admin rules. */
@WebMvcTest(ProductController.class)
class ProductControllerWebTest {
    private static final String BODY = "{\"name\":\"Phone\",\"price\":10,\"category\":{\"name\":\"E\"}}";

    @Autowired MockMvc mvc;
    @MockitoBean ProductService productService;
    @MockitoBean AuthService authService;

    private static AuthUserDto user(Long id, String... roles) {
        AuthUserDto u = new AuthUserDto();
        u.setId(id);
        u.setRoles(List.of(roles));
        return u;
    }

    @Test
    void searchIsPublicAndReportsEngine() throws Exception {
        Product p = new Product();
        p.setName("Smart Phone");
        when(productService.searchProducts("phone")).thenReturn(new ProductSearchResult(List.of(p), "elasticsearch"));

        mvc.perform(get("/api/v1/products/search").param("keyword", "phone"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Search-Engine", "elasticsearch"))
                .andExpect(jsonPath("$[0].name").value("Smart Phone"));
    }

    @Test
    void writeWithoutTokenIs401() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(productService, never()).addProduct(any());
    }

    @Test
    void writeWithInvalidTokenIs401() throws Exception {
        when(authService.validateToken("bad")).thenThrow(new UnauthorizedException("Invalid or expired token"));
        mvc.perform(post("/api/v1/products").header("Authorization", "Bearer bad")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeByCustomerIs403() throws Exception {
        when(authService.validateToken("cust")).thenReturn(user(2L, "CUSTOMER"));
        mvc.perform(post("/api/v1/products").header("Authorization", "Bearer cust")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Administrator role required to change products"));
        verify(productService, never()).addProduct(any());
    }

    @Test
    void writeByAdminSucceedsAndRecordsCreator() throws Exception {
        when(authService.validateToken("adm")).thenReturn(user(1L, "CUSTOMER", "ADMIN"));
        when(productService.addProduct(any())).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/v1/products").header("Authorization", "Bearer adm")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }
}
