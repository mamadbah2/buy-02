package sn.dev.product_service.web.controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import sn.dev.product_service.web.dto.ProductCreateDTO;
import sn.dev.product_service.web.dto.ProductResponseDTO;
import sn.dev.product_service.web.dto.ProductUpdateDTO;

@RequestMapping("/api/products")
public interface ProductController {
    @PreAuthorize("hasAuthority('SELLER')")
    @PostMapping
    ResponseEntity<ProductResponseDTO> create(@ModelAttribute @Valid ProductCreateDTO productCreateDTO);

    @GetMapping
    ResponseEntity<Page<ProductResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection);

    @GetMapping("/{id}")
    ResponseEntity<ProductResponseDTO> getById(@PathVariable String id);

    @GetMapping("/seller/{sellerId}")
    ResponseEntity<Page<ProductResponseDTO>> getBySellerId(
            @PathVariable String sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection);

    @GetMapping("/search")
    ResponseEntity<Page<ProductResponseDTO>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @GetMapping("/suggest")
    java.util.List<String> suggest(@RequestParam(name = "query") String query);

    @PreAuthorize("hasAuthority('SELLER')")
    @PutMapping("/{id}")
    ResponseEntity<ProductResponseDTO> update(@ModelAttribute @Valid ProductUpdateDTO productUpdateDTO,
            @PathVariable String id);

    @PreAuthorize("hasAuthority('SELLER')")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id);
}
