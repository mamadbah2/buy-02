package sn.dev.product_service.data.repo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import sn.dev.product_service.data.entities.Product;

public interface ProductRepo extends MongoRepository<Product, String> {
    // Paged variant
    Page<Product> findByUserId(String userId, Pageable pageable);
    // Non-paged variant for backward compatibility and tests
    List<Product> findByUserId(String userId);

    void deleteByUserId(String userId);
}
