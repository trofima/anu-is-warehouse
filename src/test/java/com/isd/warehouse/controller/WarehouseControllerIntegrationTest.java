package com.isd.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isd.warehouse.entities.Product;
import com.isd.warehouse.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WarehouseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/products - Create Product")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully when valid data is provided")
        void shouldCreateProductSuccessfully() throws Exception {
            String requestBody = """
                {
                    "name": "Laptop",
                    "description": "High-end gaming laptop",
                    "price": 1299.99,
                    "category": "Electronics",
                    "inStock": true
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.description", is("High-end gaming laptop")))
                .andExpect(jsonPath("$.price", is(1299.99)))
                .andExpect(jsonPath("$.category", is("Electronics")))
                .andExpect(jsonPath("$.inStock", is(true)));
        }

        @Test
        @DisplayName("should create product in repository")
        void shouldCreateProductInRepository() throws Exception {
            String requestBody = """
                {
                    "name": "Laptop",
                    "description": "High-end gaming laptop",
                    "price": 1299.99,
                    "category": "Electronics",
                    "inStock": true
                }
                """;

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody));

            // Verify the product was saved in the database
            assertEquals(1, productRepository.count());
            Product savedProduct = productRepository.findAll().getFirst();
            assertEquals("Laptop", savedProduct.getName());
            assertEquals("High-end gaming laptop", savedProduct.getDescription());
            assertEquals(0, new BigDecimal("1299.99").compareTo(savedProduct.getPrice()));
            assertEquals("Electronics", savedProduct.getCategory());
            assertFalse(savedProduct.isInStock() == false);
        }

        @Test
        @DisplayName("should create product with minimal required fields")
        void shouldCreateProductWithMinimalFields() throws Exception {
            String requestBody = """
                {
                    "name": "Basic Item",
                    "price": 9.99,
                    "inStock": false
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Basic Item")))
                .andExpect(jsonPath("$.price", is(9.99)))
                .andExpect(jsonPath("$.inStock", is(false)));

            assertEquals(1, productRepository.count());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
            String requestBody = """
                {
                    "name": "",
                    "price": 99.99,
                    "inStock": true
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());

            assertEquals(0, productRepository.count());
        }

        @Test
        @DisplayName("should return 400 when name is null")
        void shouldReturnBadRequestWhenNameIsNull() throws Exception {
            String requestBody = """
                {
                    "name": null,
                    "price": 99.99,
                    "inStock": true
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());

            assertEquals(0, productRepository.count());
        }

        @Test
        @DisplayName("should return 400 when price is null")
        void shouldReturnBadRequestWhenPriceIsNull() throws Exception {
            String requestBody = """
                {
                    "name": "Test Product",
                    "price": null,
                    "inStock": true
                }
                """;

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());

            assertEquals(0, productRepository.count());
        }

        @Test
        @DisplayName("should return 400 when request body is empty")
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());

            assertEquals(0, productRepository.count());
        }

        @Test
        @DisplayName("should return 415 when content type is not JSON")
        void shouldReturnUnsupportedMediaTypeWhenContentTypeIsNotJson() throws Exception {
            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("name=Test&price=99.99"))
                .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id} - Get Product by ID")
    class GetProductById {

        @Test
        @DisplayName("should return product when valid ID is provided")
        void shouldReturnProductWhenValidId() throws Exception {
            Product product = new Product();
            product.setName("Smartphone");
            product.setDescription("Latest model");
            product.setPrice(new BigDecimal("699.99"));
            product.setCategory("Electronics");
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(get("/api/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Smartphone")))
                .andExpect(jsonPath("$.description", is("Latest model")))
                .andExpect(jsonPath("$.price", is(699.99)))
                .andExpect(jsonPath("$.category", is("Electronics")))
                .andExpect(jsonPath("$.inStock", is(true)));
        }

        @Test
        @DisplayName("should return product with null optional fields")
        void shouldReturnProductWithNullOptionalFields() throws Exception {
            Product product = new Product();
            product.setName("Basic Item");
            product.setPrice(new BigDecimal("19.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(get("/api/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Basic Item")))
                .andExpect(jsonPath("$.price", is(19.99)))
                .andExpect(jsonPath("$.inStock", is(false)));
        }

        @Test
        @DisplayName("should return 404 when product does not exist")
        void shouldReturn404WhenProductDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when non-numeric ID is provided")
        void shouldReturn404WhenNonNumericIdProvided() throws Exception {
            mockMvc.perform(get("/api/products/invalid-id"))
                .andExpect(status().isBadRequest());
        }
    }
}