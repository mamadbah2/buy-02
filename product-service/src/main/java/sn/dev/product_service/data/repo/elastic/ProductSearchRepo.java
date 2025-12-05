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

    Page<Product> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);

    Page<Product> findByPriceGreaterThanEqual(Double minPrice, Pageable pageable);

    Page<Product> findByPriceLessThanEqual(Double maxPrice, Pageable pageable);

    /**
     * Autocomplete search-as-you-type on product name using bool_prefix across generated subfields.
     */
    @Query("{\n  \"multi_match\": {\n    \"query\": \"?0\",\n    \"type\": \"bool_prefix\",\n    \"fields\": [\n      \"name\",\n      \"name._2gram\",\n      \"name._3gram\"\n    ]\n  }\n}")
    Page<Product> customAutocompleteSearch(String query, Pageable pageable);

    /**
     * Full search with query and price range filter using a custom Elasticsearch query.
     */
    @Query("""
    {
      "bool": {
        "must": [
          {
            "multi_match": {
              "query": "?0",
              "fields": ["name^2", "description"],
              "type": "best_fields",
              "operator": "and"
            }
          }
        ],
        "filter": [
          { "range": { "price": { "gte": ?1, "lte": ?2 } } }
        ]
      }
    }
    """)
    Page<Product> searchByQueryAndPriceRange(String query, Double minPrice, Double maxPrice, Pageable pageable);

    @Query("""
    {
      "multi_match": {
        "query": "?0",
        "fields": ["name^2", "description"],
        "type": "best_fields",
        "operator": "and"
      }
    }
    """)
    Page<Product> searchByQuery(String query, Pageable pageable);
}
