package sn.dev.product_service.data.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import sn.dev.product_service.data.entities.Product;

public interface ProductRepo extends MongoRepository<Product, String> {
    Page<Product> findByUserId(String userId, Pageable pageable);
    void deleteByUserId(String userId);
}
