package sn.dev.product_service.data.repo.elastic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import sn.dev.product_service.data.entities.Product;

public interface ProductSearchRepo extends ElasticsearchRepository<Product, String> {

    /**
     * Recherche par nom ou description avec filtre de prix
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"description\"], \"type\": \"phrase_prefix\"}}], \"filter\": [{\"range\": {\"price\": {\"gte\": ?1, \"lte\": ?2}}}]}}")
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndPriceBetween(
            String query, String description, Double minPrice, Double maxPrice, Pageable pageable);

    /**
     * Recherche par nom ou description sans filtre de prix
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"description\"], \"type\": \"phrase_prefix\"}}")
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String query, String description, Pageable pageable);

    /**
     * Recherche avec prix minimum seulement
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"description\"], \"type\": \"phrase_prefix\"}}], \"filter\": [{\"range\": {\"price\": {\"gte\": ?1}}}]}}")
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndPriceGreaterThanEqual(
            String query, String description, Double minPrice, Pageable pageable);

    /**
     * Recherche avec prix maximum seulement
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"description\"], \"type\": \"phrase_prefix\"}}], \"filter\": [{\"range\": {\"price\": {\"lte\": ?1}}}]}}")
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndPriceLessThanEqual(
            String query, String description, Double maxPrice, Pageable pageable);

    Page<Product> findByPriceGreaterThanEqual(Double priceIsGreaterThan, Pageable pageable);

    Page<Product> findByPriceLessThanEqual(Double priceIsLessThan, Pageable pageable);

    Page<Product> findByPriceBetween(Double priceAfter, Double priceBefore, Pageable pageable);

    @Query("{\"range\": {\"price\": {\"gte\": ?0, \"lte\": ?1}}}")
    Page<Product> searchByPriceRange(Double minPrice, Double maxPrice, Pageable pageable);

    /**
     * Autocomplete search-as-you-type on product name using bool_prefix across generated subfields.
     */
    @Query("{\n  \"multi_match\": {\n    \"query\": \"?0\",\n    \"type\": \"bool_prefix\",\n    \"fields\": [\n      \"name\",\n      \"name._2gram\",\n      \"name._3gram\"\n    ]\n  }\n}")
    Page<Product> customAutocompleteSearch(String query, Pageable pageable);
}

