package sn.dev.product_service.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import sn.dev.product_service.data.entities.Product;

public interface ProductService {
    Product create(Product product);

    Product update(Product product);

    Product getById(String id);

    // Non-paged overload for simple usages
    List<Product> getByUserId(String userId);

    // Paged variant for listing with pagination
    Page<Product> getByUserId(String userId, Pageable pageable);

    List<Product> getAll();
    
    Page<Product> getAll(Pageable pageable);
    
    Page<Product> search(String query, Double minPrice, Double maxPrice, Pageable pageable);

    void delete(Product product);

    void deleteByUserId(String userId);

    /**
     * Returns up to {@code limit} product name suggestions for the given query using search-as-you-type.
     */
    List<String> suggest(String query, int limit);
}
