package sn.dev.product_service.data.repo.elastic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import sn.dev.product_service.data.entities.Product;

public interface ProductSearchRepo extends ElasticsearchRepository<Product, String> {

    /**
     * Recherche par nom ou description avec filtre de prix
     */
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndPriceBetween(
            String name, String description, Double minPrice, Double maxPrice, Pageable pageable);

    /**
     * Recherche par nom ou description sans filtre de prix
     */
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    /**
     * Recherche avec prix minimum seulement
     */
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndPriceGreaterThanEqual(
            String name, String description, Double minPrice, Pageable pageable);

    /**
     * Recherche avec prix maximum seulement
     */
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndPriceLessThanEqual(
            String name, String description, Double maxPrice, Pageable pageable);
}

