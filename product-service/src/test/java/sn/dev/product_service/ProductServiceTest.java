package sn.dev.product_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.data.repo.ProductRepo;
import sn.dev.product_service.data.repo.elastic.ProductSearchRepo;
import sn.dev.product_service.services.impl.ProductServiceImpl;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepo productRepo;

    @Mock
    private ProductSearchRepo productSearchRepo;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void testCreateProduct() {
        Product product = new Product();
        product.setId(UUID.randomUUID().toString());
        product.setName("Test Product");
        product.setDescription("This is a test product.");
        product.setPrice(19.99);
        product.setQuantity(100);
        product.setUserId("user123");

        // Mock the behavior of the repository
        when(productRepo.save(product)).thenReturn(product);

        // Call the service method
        Product createdProduct = productService.create(product);

        // Verify the result
        assertNotNull(createdProduct);
        assertEquals("Test Product", createdProduct.getName());
        assertEquals("This is a test product.", createdProduct.getDescription());
        assertEquals(19.99, createdProduct.getPrice());
        assertEquals(100, createdProduct.getQuantity());
        assertEquals("user123", createdProduct.getUserId());

        // Verify that the repository was called
        verify(productRepo, times(1)).save(product);

        System.out.println("✅ PRODUCT/SERVICE : testCreateProduct() passed successfully.");
    }

    @Test
    void testGetProductById() {
        String productId = UUID.randomUUID().toString();
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setDescription("This is a test product.");
        product.setPrice(19.99);
        product.setQuantity(100);
        product.setUserId("user123");

        // Mock the behavior of the repository
        when(productRepo.findById(productId)).thenReturn(Optional.of(product));

        // Call the service method
        Product foundProduct = productService.getById(productId);

        // Verify the result
        assertNotNull(foundProduct);
        assertEquals(productId, foundProduct.getId());
        assertEquals("Test Product", foundProduct.getName());
        assertEquals("This is a test product.", foundProduct.getDescription());
        assertEquals(19.99, foundProduct.getPrice());
        assertEquals(100, foundProduct.getQuantity());
        assertEquals("user123", foundProduct.getUserId());

        // Verify that the repository was called
        verify(productRepo, times(1)).findById(productId);

        System.out.println("✅ PRODUCT/SERVICE : testGetProductById() passed successfully.");
    }

    @Test
    void testGetByUserId_EmptyResult() {
        String userId = "nonexistentUser";
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepo.findByUserId(userId, pageable)).thenReturn(Page.empty());

        Page<Product> products = productService.getByUserId(userId, pageable);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        System.out.println("✅ PRODUCT/SERVICE : testGetByUserId_EmptyResult() passed successfully.");
    }

    @Test
    void testGetProductById_NotFound() {
        String productId = UUID.randomUUID().toString();

        // Mock repository to return empty
        when(productRepo.findById(productId)).thenReturn(Optional.empty());

        // Assert that a ResponseStatusException is thrown
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            productService.getById(productId);
        });

        // Optionally verify exception message and status
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        // Verify interaction with repository
        verify(productRepo, times(1)).findById(productId);

        System.out.println("✅ PRODUCT/SERVICE : testGetProductById_NotFound() passed successfully.");
    }

    @Test
    void testGetAllProducts() {

        // Arrange
        List<Product> mockProducts = List.of(
                new Product("Product A", "Description A", 10.0, 50, "user1"),
                new Product("Product B", "Description B", 20.0, 30, "user2"),
                new Product("Product C", "Description C", 30.0, 20, "user3"));

        when(productRepo.findAll()).thenReturn(mockProducts);

        // Act
        List<Product> products = productService.getAll();

        // Assert
        assertNotNull(products);
        assertEquals(3, products.size());
        assertEquals("Product A", products.get(0).getName());
        assertEquals("Product B", products.get(1).getName());
        assertEquals("Product C", products.get(2).getName());
        assertEquals(10.0, products.get(0).getPrice());
        assertEquals(20.0, products.get(1).getPrice());
        assertEquals(30.0, products.get(2).getPrice());

        // Verify interaction with repository
        verify(productRepo, times(1)).findAll();

        System.out.println("✅ PRODUCT/SERVICE : testGetAllProducts() passed successfully.");
    }

    @Test
    void testGetProductsByUserId() {
        String userId = "user123";

        List<Product> mockProducts = List.of(
                new Product("Product 1", "Description 1", 10.0, 5, userId),
                new Product("Product 2", "Description 2", 20.0, 10, userId));

        // Mock repository behavior
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> mockPage = new PageImpl<>(mockProducts, pageable, mockProducts.size());
        when(productRepo.findByUserId(userId, pageable)).thenReturn(mockPage);

        // Call service method
        Page<Product> userProducts = productService.getByUserId(userId, pageable);

        // Assert results
        assertNotNull(userProducts);
        assertEquals(2, userProducts.getTotalElements());
        assertEquals("Product 1", userProducts.getContent().get(0).getName());
        assertEquals("Product 2", userProducts.getContent().get(1).getName());

        // Verify repository interaction
        verify(productRepo, times(1)).findByUserId(userId, pageable);

        System.out.println("✅ PRODUCT/SERVICE : testGetProductsByUserId() passed successfully.");
    }

    @Test
    void testUpdateProduct() {
        // Given
        Product productToUpdate = new Product("Updated Name", "Updated Description", 99.99, 15, "user123");
        productToUpdate.setId("product123");

        // Mock the repository to return the product when saving
        when(productRepo.save(productToUpdate)).thenReturn(productToUpdate);

        // When
        Product updatedProduct = productService.update(productToUpdate);

        // Then
        assertNotNull(updatedProduct);
        assertEquals("Updated Name", updatedProduct.getName());
        assertEquals("Updated Description", updatedProduct.getDescription());
        assertEquals(99.99, updatedProduct.getPrice());
        assertEquals(15, updatedProduct.getQuantity());
        assertEquals("user123", updatedProduct.getUserId());

        // Verify that save was called
        verify(productRepo, times(1)).save(productToUpdate);

        System.out.println("✅ PRODUCT/SERVICE : testUpdateProduct() passed successfully.");
    }

    @Test
    void testDeleteProduct() {
        // Given
        Product productToDelete = new Product();
        productToDelete.setId("product123");
        productToDelete.setName("To Be Deleted");
        productToDelete.setUserId("user123");

        // When
        productService.delete(productToDelete);

        // Then
        verify(productRepo, times(1)).delete(productToDelete);

        System.out.println("✅ PRODUCT/SERVICE : testDeleteProduct() passed successfully.");
    }

    @Test
    void testDeleteByUserId() {
        // Given
        String userId = "user123";

        // When
        productService.deleteByUserId(userId);

        // Then
        verify(productRepo, times(1)).deleteByUserId(userId);

        System.out.println("✅ PRODUCT/SERVICE : testDeleteByUserId() passed successfully.");
    }

}