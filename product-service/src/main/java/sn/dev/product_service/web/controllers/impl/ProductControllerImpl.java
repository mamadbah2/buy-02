package sn.dev.product_service.web.controllers.impl;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.dev.product_service.data.entities.Media;
import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.services.MediaServiceClient;
import sn.dev.product_service.services.ProductService;
import sn.dev.product_service.web.controllers.ProductController;
import sn.dev.product_service.web.dto.ProductCreateDTO;
import sn.dev.product_service.web.dto.ProductResponseDTO;
import sn.dev.product_service.web.dto.ProductUpdateDTO;

@RestController
@RequiredArgsConstructor
public class ProductControllerImpl implements ProductController {

    private final ProductService productService;
    private final MediaServiceClient mediaServiceClient;
    private String maxAge = "300";
    private static final String USERIDSTR = "userID";

    @Override
    public ResponseEntity<ProductResponseDTO> create(
        @Valid ProductCreateDTO productCreateDTO
    ) {
        System.out.println("CREATE product : " + productCreateDTO);

        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        Product product = productService.create(
            productCreateDTO.toProduct(userId)
        );
        List<Media> medias = productCreateDTO
            .getImages()
            .stream()
            .map(file -> mediaServiceClient.upload(file, product.getId()))
            .toList();

        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(new ProductResponseDTO(product, medias));
    }

    @Override
    public ResponseEntity<Page<ProductResponseDTO>> getAll(
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {
        System.out.println(
            String.format(
                "GET(getAll) products - page: %d, size: %d, sortBy: %s, direction: %s",
                page, size, sortBy, sortDirection
            )
        );

        // Créer l'objet Sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // Créer l'objet Pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Récupérer les produits paginés
        Page<Product> productsPage = productService.getAll(pageable);

        // Mapper vers ProductResponseDTO
        Page<ProductResponseDTO> responsePage = productsPage.map(product -> {
            List<Media> medias = mediaServiceClient
                .getByProductId(product.getId())
                .getBody();
            return new ProductResponseDTO(product, medias);
        });

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(responsePage);
    }

    @Override
    public ResponseEntity<ProductResponseDTO> getById(String id) {
        System.out.println("GET(product by id) product with id: " + id);

        Product product = productService.getById(id);
        List<Media> medias = mediaServiceClient.getByProductId(id).getBody();

        return ResponseEntity.ok(new ProductResponseDTO(product, medias));
    }

    @Override
    public ResponseEntity<Page<ProductResponseDTO>> getBySellerId(
        String sellerId,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {
        System.out.println(
            String.format(
                "GET(products by seller) sellerId: %s - page: %d, size: %d, sortBy: %s, direction: %s",
                sellerId, page, size, sortBy, sortDirection
            )
        );

        // Créer l'objet Sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // Créer l'objet Pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Récupérer les produits du vendeur paginés
        Page<Product> productsPage = productService.getByUserId(sellerId, pageable);

        // Mapper vers ProductResponseDTO
        Page<ProductResponseDTO> responsePage = productsPage.map(product -> {
            List<Media> medias = mediaServiceClient
                .getByProductId(product.getId())
                .getBody();
            return new ProductResponseDTO(product, medias);
        });

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(responsePage);
    }

    @Override
    public ResponseEntity<Page<ProductResponseDTO>> search(
        String query,
        Double minPrice,
        Double maxPrice,
        int page,
        int size
    ) {
        System.out.println(
            String.format(
                "SEARCH products - query: '%s', minPrice: %s, maxPrice: %s, page: %d, size: %d",
                query, minPrice, maxPrice, page, size
            )
        );

        // Créer l'objet Pageable
        Pageable pageable = PageRequest.of(page, size);

        // Rechercher les produits
        Page<Product> productsPage = productService.search(query, minPrice, maxPrice, pageable);

        // Mapper vers ProductResponseDTO
        Page<ProductResponseDTO> responsePage = productsPage.map(product -> {
            List<Media> medias = mediaServiceClient
                .getByProductId(product.getId())
                .getBody();
            return new ProductResponseDTO(product, medias);
        });

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(responsePage);
    }

    @Override
    public ResponseEntity<ProductResponseDTO> update(
        @Valid ProductUpdateDTO productUpdateDTO,
        String id
    ) {
        System.out.println("UPDATE(product by id) product with id: " + id);

        Product product = productService.getById(id);
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        if (!product.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to update this product"
            );
        }

        Product productToUpdate = productUpdateDTO.toProduct(userId);
        productToUpdate.setId(id);

        Product updatedProduct = productService.update(productToUpdate);

        // Handle new images if provided
        if (
            productUpdateDTO.getImages() != null &&
            !productUpdateDTO.getImages().isEmpty()
        ) {
            System.out.println(
                "Uploading " +
                productUpdateDTO.getImages().size() +
                " new images for product " +
                id
            );
            productUpdateDTO
                .getImages()
                .forEach(file -> {
                    System.out.println(
                        "Uploading image: " + file.getOriginalFilename()
                    );
                    mediaServiceClient.upload(file, updatedProduct.getId());
                });
        }

        // Get all medias (existing + newly uploaded)
        List<Media> medias = mediaServiceClient
            .getByProductId(updatedProduct.getId())
            .getBody();

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(new ProductResponseDTO(updatedProduct, medias));
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        System.out.println("DELETE(product by id) product with id: " + id);

        Product product = productService.getById(id);
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        if (!product.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to delete this product"
            );
        }

        productService.delete(product);
        mediaServiceClient.deleteByProductId(id);

        return ResponseEntity.noContent().build();
    }

    @Override
    public java.util.List<String> suggest(String query) {
        if (query == null || query.trim().length() < 2) {
            return java.util.List.of();
        }
        // limit results to 5 as requested
        return productService.suggest(query.trim(), 5);
    }
}
