package com.isd.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isd.warehouse.entities.Inventory;
import com.isd.warehouse.entities.Product;
import com.isd.warehouse.repository.InventoryRepository;
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

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        inventoryRepository.deleteAll();
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
                .andExpect(jsonPath("$.inStock", is(true)))
                .andExpect(jsonPath("$.reservedQuantity", is(0)))
                .andExpect(jsonPath("$.quantity", is(0)));
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
                .andExpect(jsonPath("$.inStock", is(false)))
                .andExpect(jsonPath("$.reservedQuantity", is(0)))
                .andExpect(jsonPath("$.quantity", is(0)));

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

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(20);
            inventory.setReservedQuantity(5);
            inventoryRepository.save(inventory);

            mockMvc.perform(get("/api/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Smartphone")))
                .andExpect(jsonPath("$.description", is("Latest model")))
                .andExpect(jsonPath("$.price", is(699.99)))
                .andExpect(jsonPath("$.category", is("Electronics")))
                .andExpect(jsonPath("$.inStock", is(true)))
                .andExpect(jsonPath("$.reservedQuantity", is(5)))
                .andExpect(jsonPath("$.quantity", is(20)));
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
                .andExpect(jsonPath("$.inStock", is(false)))
                .andExpect(jsonPath("$.reservedQuantity", is(0)))
                .andExpect(jsonPath("$.quantity", is(0)));
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

    @Nested
    @DisplayName("GET /api/products - List Products")
    class ListProducts {

        @Test
        @DisplayName("should return list of product DTOs with availability")
        void shouldReturnProductListWithAvailability() throws Exception {
            Product p1 = new Product();
            p1.setName("Item A");
            p1.setDescription("Description A");
            p1.setCategory("Category A");
            p1.setPrice(new BigDecimal("10.00"));
            p1.setInStock(true);
            p1 = productRepository.save(p1);

            Inventory i1 = new Inventory();
            i1.setProduct(p1);
            i1.setQuantity(10);
            i1.setReservedQuantity(2);
            inventoryRepository.save(i1);

            Product p2 = new Product();
            p2.setName("Item B");
            p2.setDescription("Description B");
            p2.setCategory("Category B");
            p2.setPrice(new BigDecimal("20.00"));
            p2.setInStock(false);
            p2 = productRepository.save(p2); // No inventory for p2

            mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].id", is(p1.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is("Item A")))
                .andExpect(jsonPath("$[0].description", is("Description A")))
                .andExpect(jsonPath("$[0].price", is(10.00)))
                .andExpect(jsonPath("$[0].category", is("Category A")))
                .andExpect(jsonPath("$[0].inStock", is(true)))
                .andExpect(jsonPath("$[0].reservedQuantity", is(2)))
                .andExpect(jsonPath("$[0].quantity", is(10)))
                .andExpect(jsonPath("$[1].id", is(p2.getId().intValue())))
                .andExpect(jsonPath("$[1].name", is("Item B")))
                .andExpect(jsonPath("$[1].description", is("Description B")))
                .andExpect(jsonPath("$[1].price", is(20.00)))
                .andExpect(jsonPath("$[1].category", is("Category B")))
                .andExpect(jsonPath("$[1].inStock", is(false)))
                .andExpect(jsonPath("$[1].reservedQuantity", is(0)))
                .andExpect(jsonPath("$[1].quantity", is(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/products/{id}/receive - Receive Product Stock")
    class ReceiveProductStock {

        @Test
        @DisplayName("should create inventory and receive stock for product without inventory")
        void shouldCreateInventoryAndReceiveStock() throws Exception {
            Product product = new Product();
            product.setName("Keyboard");
            product.setPrice(new BigDecimal("89.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/receive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 12
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.quantity", is(12)))
                .andExpect(jsonPath("$.reservedQuantity", is(0)))
                .andExpect(jsonPath("$.availableQuantity", is(12)));

            Inventory inventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(12, inventory.getQuantity());
            assertEquals(0, inventory.getReservedQuantity());
            assertEquals(true, productRepository.findById(savedProduct.getId()).orElseThrow().isInStock());
        }

        @Test
        @DisplayName("should add received quantity to existing inventory")
        void shouldAddReceivedQuantityToExistingInventory() throws Exception {
            Product product = new Product();
            product.setName("Mouse");
            product.setPrice(new BigDecimal("49.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(5);
            inventory.setReservedQuantity(2);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/receive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 7
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.quantity", is(12)))
                .andExpect(jsonPath("$.reservedQuantity", is(2)))
                .andExpect(jsonPath("$.availableQuantity", is(10)));

            Inventory updatedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(12, updatedInventory.getQuantity());
            assertEquals(2, updatedInventory.getReservedQuantity());
        }

        @Test
        @DisplayName("should return 404 when receiving stock for unknown product")
        void shouldReturn404WhenReceivingUnknownProduct() throws Exception {
            mockMvc.perform(post("/api/products/99999/receive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 3
                        }
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when received quantity is zero")
        void shouldReturn400WhenReceivedQuantityIsZero() throws Exception {
            Product product = new Product();
            product.setName("Dock");
            product.setPrice(new BigDecimal("129.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/receive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 0
                        }
                        """))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/products/{id}/reserve - Reserve Product Stock")
    class ReserveProductStock {

        @Test
        @DisplayName("should reserve available stock")
        void shouldReserveAvailableStock() throws Exception {
            Product product = new Product();
            product.setName("Headset");
            product.setPrice(new BigDecimal("149.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(10);
            inventory.setReservedQuantity(3);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 4
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.reservedQuantity", is(7)))
                .andExpect(jsonPath("$.availableQuantity", is(3)));

            Inventory updatedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(10, updatedInventory.getQuantity());
            assertEquals(7, updatedInventory.getReservedQuantity());
            assertEquals(true, productRepository.findById(savedProduct.getId()).orElseThrow().isInStock());
        }

        @Test
        @DisplayName("should mark product out of stock when all quantity becomes reserved")
        void shouldMarkProductOutOfStockWhenFullyReserved() throws Exception {
            Product product = new Product();
            product.setName("Webcam");
            product.setPrice(new BigDecimal("79.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(6);
            inventory.setReservedQuantity(1);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 5
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(6)))
                .andExpect(jsonPath("$.reservedQuantity", is(6)))
                .andExpect(jsonPath("$.availableQuantity", is(0)));

            assertEquals(false, productRepository.findById(savedProduct.getId()).orElseThrow().isInStock());
        }

        @Test
        @DisplayName("should return 404 when reserving stock for product without inventory")
        void shouldReturn404WhenInventoryDoesNotExist() throws Exception {
            Product product = new Product();
            product.setName("Speaker");
            product.setPrice(new BigDecimal("199.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 1
                        }
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when reservation exceeds available stock")
        void shouldReturn400WhenReservationExceedsAvailableStock() throws Exception {
            Product product = new Product();
            product.setName("Microphone");
            product.setPrice(new BigDecimal("129.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(8);
            inventory.setReservedQuantity(6);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 3
                        }
                        """))
                .andExpect(status().isBadRequest());

            Inventory unchangedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(6, unchangedInventory.getReservedQuantity());
        }

        @Test
        @DisplayName("should return 400 when reservation quantity is zero")
        void shouldReturn400WhenReservationQuantityIsZero() throws Exception {
            Product product = new Product();
            product.setName("Charger");
            product.setPrice(new BigDecimal("39.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(5);
            inventory.setReservedQuantity(0);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 0
                        }
                        """))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/products/{id}/fulfill - Fulfill Product Stock")
    class FulfillProductStock {

        @Test
        @DisplayName("should fulfill reserved stock")
        void shouldFulfillReservedStock() throws Exception {
            Product product = new Product();
            product.setName("Router");
            product.setPrice(new BigDecimal("119.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(10);
            inventory.setReservedQuantity(6);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/fulfill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 4
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.quantity", is(6)))
                .andExpect(jsonPath("$.reservedQuantity", is(2)))
                .andExpect(jsonPath("$.availableQuantity", is(4)));

            Inventory updatedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(6, updatedInventory.getQuantity());
            assertEquals(2, updatedInventory.getReservedQuantity());
            assertEquals(true, productRepository.findById(savedProduct.getId()).orElseThrow().isInStock());
        }

        @Test
        @DisplayName("should keep product out of stock when fulfilled inventory still has no available quantity")
        void shouldKeepProductOutOfStockWhenNoAvailableQuantityRemains() throws Exception {
            Product product = new Product();
            product.setName("Printer");
            product.setPrice(new BigDecimal("299.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(5);
            inventory.setReservedQuantity(5);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/fulfill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 5
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(0)))
                .andExpect(jsonPath("$.reservedQuantity", is(0)))
                .andExpect(jsonPath("$.availableQuantity", is(0)));

            assertEquals(false, productRepository.findById(savedProduct.getId()).orElseThrow().isInStock());
        }

        @Test
        @DisplayName("should return 404 when fulfilling stock for product without inventory")
        void shouldReturn404WhenInventoryDoesNotExist() throws Exception {
            Product product = new Product();
            product.setName("Scanner");
            product.setPrice(new BigDecimal("159.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/fulfill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 1
                        }
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when fulfillment exceeds reserved stock")
        void shouldReturn400WhenFulfillmentExceedsReservedStock() throws Exception {
            Product product = new Product();
            product.setName("Projector");
            product.setPrice(new BigDecimal("499.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(7);
            inventory.setReservedQuantity(2);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/fulfill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 3
                        }
                        """))
                .andExpect(status().isBadRequest());

            Inventory unchangedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(7, unchangedInventory.getQuantity());
            assertEquals(2, unchangedInventory.getReservedQuantity());
        }

        @Test
        @DisplayName("should return 400 when fulfillment quantity is zero")
        void shouldReturn400WhenFulfillmentQuantityIsZero() throws Exception {
            Product product = new Product();
            product.setName("Tablet");
            product.setPrice(new BigDecimal("229.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(4);
            inventory.setReservedQuantity(1);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/fulfill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 0
                        }
                        """))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/products/{id}/cancel-reservation - Cancel Reservation")
    class CancelReservationStock {

        @Test
        @DisplayName("should cancel reserved stock")
        void shouldCancelReservedStock() throws Exception {
            Product product = new Product();
            product.setName("Laptop Stand");
            product.setPrice(new BigDecimal("59.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(10);
            inventory.setReservedQuantity(8);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/cancel-reservation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 3
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.reservedQuantity", is(5)))
                .andExpect(jsonPath("$.availableQuantity", is(5)));

            Inventory updatedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(10, updatedInventory.getQuantity());
            assertEquals(5, updatedInventory.getReservedQuantity());
            assertEquals(true, productRepository.findById(savedProduct.getId()).orElseThrow().isInStock());
        }

        @Test
        @DisplayName("should keep product out of stock when canceled reservation still leaves no available quantity")
        void shouldKeepProductOutOfStockWhenNoAvailableQuantityRemains() throws Exception {
            Product product = new Product();
            product.setName("Monitor Arm");
            product.setPrice(new BigDecimal("89.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(5);
            inventory.setReservedQuantity(5);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/cancel-reservation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 0
                        }
                        """))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when canceling reservation for product without inventory")
        void shouldReturn404WhenInventoryDoesNotExist() throws Exception {
            Product product = new Product();
            product.setName("USB Hub");
            product.setPrice(new BigDecimal("34.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/cancel-reservation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 1
                        }
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when cancellation exceeds reserved stock")
        void shouldReturn400WhenCancellationExceedsReservedStock() throws Exception {
            Product product = new Product();
            product.setName("Desk Lamp");
            product.setPrice(new BigDecimal("44.99"));
            product.setInStock(true);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(7);
            inventory.setReservedQuantity(2);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/cancel-reservation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 3
                        }
                        """))
                .andExpect(status().isBadRequest());

            Inventory unchangedInventory = inventoryRepository.findByProductId(savedProduct.getId()).orElseThrow();
            assertEquals(7, unchangedInventory.getQuantity());
            assertEquals(2, unchangedInventory.getReservedQuantity());
        }

        @Test
        @DisplayName("should return 400 when cancellation quantity is zero")
        void shouldReturn400WhenCancellationQuantityIsZero() throws Exception {
            Product product = new Product();
            product.setName("Docking Station");
            product.setPrice(new BigDecimal("179.99"));
            product.setInStock(false);
            Product savedProduct = productRepository.save(product);

            Inventory inventory = new Inventory();
            inventory.setProduct(savedProduct);
            inventory.setQuantity(4);
            inventory.setReservedQuantity(4);
            inventoryRepository.save(inventory);

            mockMvc.perform(post("/api/products/" + savedProduct.getId() + "/cancel-reservation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "quantity": 0
                        }
                        """))
                .andExpect(status().isBadRequest());
        }
    }
}
