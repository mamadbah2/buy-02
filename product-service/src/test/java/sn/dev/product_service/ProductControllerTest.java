package sn.dev.product_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import sn.dev.product_service.data.entities.Media;
import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.services.MediaServiceClient;
import sn.dev.product_service.services.ProductService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {
    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private MediaServiceClient mediaServiceClient;

    @Autowired
    private MockMvc mockMvc;

    private Jwt fakeJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("userID", "user-123")
                .build();
    }

    @Test
    @WithMockUser
    void testGetAllReturnsProductResponseDTOList() throws Exception {
        Product product = new Product("1", "Test Product");
        Media media1 = new Media("m1", "image1.png");
        Media media2 = new Media("m2", "image2.png");
        Media media3 = new Media("m3", "image3.png");

        // Mock service responses
        when(productService.getAll()).thenReturn(List.of(product));
        when(mediaServiceClient.getByProductId("1"))
                .thenReturn(ResponseEntity.ok(List.of(media1, media2, media3)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"))
                .andExpect(jsonPath("$[0].images").isArray())
                .andExpect(jsonPath("$[0].images.length()").value(3))
                .andExpect(jsonPath("$[0].images[0].imageUrl").value("image1.png"))
                .andExpect(jsonPath("$[0].images[1].imageUrl").value("image2.png"))
                .andExpect(jsonPath("$[0].images[2].imageUrl").value("image3.png"));

        System.out.println(
                "✅ PRODUCT/CONTROLLER : testGetAllReturnsProductResponseDTOList() passed successfully.");
    }

    @Test
    @WithMockUser
    void testCreateProduct() throws Exception {
        // Prepare multipart files
        MockMultipartFile image1 = new MockMultipartFile(
                "images", // matches field name in DTO
                "image1.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content-1".getBytes(StandardCharsets.UTF_8));

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "image2.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content-2".getBytes(StandardCharsets.UTF_8));

        // Mock service layer
        Product savedProduct = new Product("New Product", "Description", 100.0, 5, "user-123");
        savedProduct.setId("1"); // Mock ID after save

        when(productService.create(any(Product.class))).thenReturn(savedProduct);

        when(mediaServiceClient.upload(
                any(MultipartFile.class),
                eq("1")))
                .thenAnswer(invocation -> {
                    MultipartFile file = invocation.getArgument(0);
                    if (file == null) {
                        return new Media("m0", "null-file"); // Handle null case
                    }
                    switch (file.getOriginalFilename()) {
                        case null:
                            return new Media("m0", "null-file");
                        case "image1.png":
                            return new Media("m1", "image1.png");
                        case "image2.png":
                            return new Media("m2", "image2.png");
                        default:
                            return new Media("mX", file.getOriginalFilename());
                    }
                });
        // Perform multipart request
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products")
                                .file(image1)
                                .file(image2)
                                .param("name", "New Product")
                                .param("description", "Description")
                                .param("price", "100.0")
                                .param("quantity", "5")
                                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(fakeJwt())
                                        .authorities(new SimpleGrantedAuthority("SELLER")))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=300")) // adjust
                // if
                // needed
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("New Product"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.price").value(100.0))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].imageUrl").value("image1.png"))
                .andExpect(jsonPath("$.images[1].imageUrl").value("image2.png"));

        System.out.println("✅ PRODUCT/CONTROLLER : testCreateProduct() passed successfully.");
    }

    @Test
    void testCreateProductFailsWhenNameMissing() throws Exception {
        MockMultipartFile image1 = new MockMultipartFile(
                "images", "image1.png", MediaType.IMAGE_PNG_VALUE,
                "img1".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products")
                                .file(image1)
                                .param("description", "Description")
                                .param("price", "100.0")
                                .param("quantity", "5")
                                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(fakeJwt()))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        System.out.println(
                "✅ PRODUCT/CONTROLLER : testCreateProductFailsWhenNameMissing() passed successfully.");
    }

    @Test
    void testCreateProductFailsWhenNoImages() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products")
                                .param("name", "New Product")
                                .param("description", "Description")
                                .param("price", "100.0")
                                .param("quantity", "5")
                                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(fakeJwt()))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        System.out.println(
                "✅ PRODUCT/CONTROLLER : testCreateProductFailsWhenNoImages() passed successfully.");
    }

    @Test
    @WithMockUser
    void testGetProductByIdReturnsProductResponseDTO() throws Exception {
        String productId = "1";

        Product product = new Product("Test Product", "Description", 100.0, 5, "user-123");
        product.setId(productId);
        Media media1 = new Media("m1", "image1.png");
        Media media2 = new Media("m2", "image2.png");

        // Mock service calls
        when(productService.getById(productId)).thenReturn(product);
        when(mediaServiceClient.getByProductId(productId))
                .thenReturn(ResponseEntity.ok(List.of(media1, media2)));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.price").value(100.0))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].imageUrl").value("image1.png"))
                .andExpect(jsonPath("$.images[1].imageUrl").value("image2.png"));

        System.out.println(
                "✅ PRODUCT/CONTROLLER : testGetProductByIdReturnsProductResponseDTO() passed successfully.");
    }

    @Test
    void testUpdateProduct_Success() throws Exception {
        String productId = "1";
        String userId = "user-123";

        // Existing product owned by user-123
        Product existingProduct = new Product("Old Name", "Old Desc", 50.0, 2, userId);
        existingProduct.setId(productId);
        // Updated product DTO
        MockMultipartFile image1 = new MockMultipartFile(
                "images", "new-image.png", MediaType.IMAGE_PNG_VALUE,
                "new-image-content".getBytes(StandardCharsets.UTF_8));

        when(productService.getById(productId)).thenReturn(existingProduct);

        Product updatedProduct = new Product("New Name", "New Desc", 100.0, 5, userId);
        updatedProduct.setId(productId);
        when(productService.update(any(Product.class))).thenReturn(updatedProduct);

        // Mock uploading new image
        when(mediaServiceClient.upload(any(MultipartFile.class), eq(productId)))
                .thenReturn(new Media("m1", "new-image.png"));

        // Mock returning all medias including new one
        when(mediaServiceClient.getByProductId(productId))
                .thenReturn(ResponseEntity.ok(List.of(new Media("m1", "new-image.png"),
                        new Media("m2", "existing-image.png"))));

        // Perform multipart PATCH or PUT request (adjust to your controller mapping)
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products/{id}", productId)
                                .file(image1)
                                .param("name", "New Name")
                                .param("description", "New Desc")
                                .param("price", "100.0")
                                .param("quantity", "5")
                                .with(request -> { // PATCH or PUT method workaround with multipart
                                    request.setMethod("PUT"); // or "PATCH" depending on your
                                    // mapping
                                    return request;
                                })
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("userID", userId)
                                                .build())
                                        .authorities(new SimpleGrantedAuthority("SELLER")))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=300")) // adjust
                // if
                // needed
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.price").value(100.0))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].imageUrl").value("new-image.png"))
                .andExpect(jsonPath("$.images[1].imageUrl").value("existing-image.png"));

        System.out.println("✅ PRODUCT/CONTROLLER : testUpdateProduct_Success() passed successfully.");
    }

    @Test
    void testUpdateProduct_ForbiddenWhenUserIdMismatch() throws Exception {
        String productId = "1";
        String ownerUserId = "user-123";
        String jwtUserId = "another-user";

        Product existingProduct = new Product("Old Name", "Old Desc", 50.0, 2, ownerUserId);
        existingProduct.setId(productId);

        when(productService.getById(productId)).thenReturn(existingProduct);

        MockMultipartFile image1 = new MockMultipartFile(
                "images", "new-image.png", MediaType.IMAGE_PNG_VALUE,
                "new-image-content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products/{id}", productId)
                                .file(image1)
                                .param("name", "New Name")
                                .param("description", "New Desc")
                                .param("price", "100.0")
                                .param("quantity", "5")
                                .with(request -> {
                                    request.setMethod("PUT");
                                    return request;
                                })
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("userID", jwtUserId)
                                                .build())
                                        .authorities(new SimpleGrantedAuthority("SELLER")))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        System.out.println(
                "✅ PRODUCT/CONTROLLER : testUpdateProduct_ForbiddenWhenUserIdMismatch() passed successfully.");
    }

    @Test
    void testUpdateProduct_NoImages() throws Exception {
        String productId = "1";
        String userId = "user-123";

        Product existingProduct = new Product("Old Name", "Old Desc", 50.0, 2, userId);
        existingProduct.setId(productId);
        Product updatedProduct = new Product("Updated Name", "Updated Desc", 80.0, 3, userId);
        updatedProduct.setId(productId);

        when(productService.getById(productId)).thenReturn(existingProduct);
        when(productService.update(any(Product.class))).thenReturn(updatedProduct);
        when(mediaServiceClient.getByProductId(productId))
                .thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products/{id}", productId)
                                // No files
                                .param("name", "Updated Name")
                                .param("description", "Updated Desc")
                                .param("price", "80.0")
                                .param("quantity", "3")
                                .with(request -> {
                                    request.setMethod("PUT");
                                    return request;
                                })
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("userID", userId)
                                                .build())
                                        .authorities(new SimpleGrantedAuthority("SELLER")))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated Desc"))
                .andExpect(jsonPath("$.price").value(80.0))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    void testUpdateProductFailsWhenNameMissing() throws Exception {
        String productId = "1";
        String userId = "user-123";

        Product existingProduct = new Product("Old Name", "Old Desc", 50.0, 2, userId);
        existingProduct.setId(productId);

        when(productService.getById(productId)).thenReturn(existingProduct);

        MockMultipartFile image1 = new MockMultipartFile(
                "images", "image1.png", MediaType.IMAGE_PNG_VALUE,
                "img1".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/products/{id}", productId)
                                .file(image1)
                                // Missing "name" param intentionally
                                .param("description", "Updated Desc")
                                .param("price", "80.0")
                                .param("quantity", "3")
                                .with(request -> {
                                    request.setMethod("PUT");
                                    return request;
                                })
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("userID", userId)
                                                .build())
                                        .authorities(new SimpleGrantedAuthority("SELLER")))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        System.out.println(
                "✅ PRODUCT/CONTROLLER : testUpdateProductFailsWhenNameMissing() passed successfully.");
    }

    @Test
    void testDeleteProduct_Success() throws Exception {
        String productId = "1";
        String userId = "user-123";

        Product product = new Product("Product Name", "Description", 100.0, 5, userId);
        product.setId(productId);

        when(productService.getById(productId)).thenReturn(product);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/products/{id}", productId)
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("userID", userId)
                                                .build())
                                        .authorities(new SimpleGrantedAuthority("SELLER"))))
                .andExpect(status().isNoContent());

        // Verify that delete methods were called
        org.mockito.Mockito.verify(productService).delete(product);
        org.mockito.Mockito.verify(mediaServiceClient).deleteByProductId(productId);
    }

    @Test
    void testDeleteProduct_ForbiddenWhenUserIdMismatch() throws Exception {
        String productId = "1";
        String ownerUserId = "user-123";
        String jwtUserId = "another-user";

        Product product = new Product("Product Name", "Description", 100.0, 5, ownerUserId);
        product.setId(productId);

        when(productService.getById(productId)).thenReturn(product);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/products/{id}", productId)
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("userID", jwtUserId)
                                                .build())
                                        .authorities(new SimpleGrantedAuthority("SELLER"))))
                .andExpect(status().isForbidden());

        // Verify no delete calls
        org.mockito.Mockito.verify(productService, org.mockito.Mockito.never())
                .delete(org.mockito.Mockito.any());
        org.mockito.Mockito.verify(mediaServiceClient, org.mockito.Mockito.never())
                .deleteByProductId(org.mockito.Mockito.anyString());
    }

}