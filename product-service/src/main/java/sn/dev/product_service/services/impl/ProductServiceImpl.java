package sn.dev.product_service.services.impl;

import java.util.List;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.data.repo.ProductRepo;
import sn.dev.product_service.data.repo.elastic.ProductSearchRepo;
import sn.dev.product_service.services.ProductService;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepo productRepo;
    private final ProductSearchRepo productSearchRepo;

    @Override
    public Product create(Product product) {
        Product savedProduct = productRepo.save(product);
        // Indexer dans ElasticSearch
        productSearchRepo.save(savedProduct);
        return savedProduct;
    }

    @Override
    public List<Product> getAll() {
        return productRepo.findAll();
    }

    @Override
    public Page<Product> getAll(Pageable pageable) {
        return productRepo.findAll(pageable);
    }

    @Override
    public Product getById(String id) {
        return productRepo.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id));
    }

    @Override
    public List<Product> getByUserId(String userId) {
        return productRepo.findByUserId(userId);
    }

    @Override
    public Page<Product> getByUserId(String userId, Pageable pageable) {
        return productRepo.findByUserId(userId, pageable);
    }

    @Override
    public Page<Product> search(String query, Double minPrice, Double maxPrice, Pageable pageable) {
        String sanitizedQuery = null;
        if (query != null && !query.trim().isEmpty()) {
            sanitizedQuery = query.trim();
        }

        if (sanitizedQuery == null) {
            if (minPrice != null && maxPrice != null) {
                return productSearchRepo.findByPriceBetween(minPrice, maxPrice, pageable);
            } else if (minPrice != null) {
                return productSearchRepo.findByPriceGreaterThanEqual(minPrice, pageable);
            } else if (maxPrice != null) {
                return productSearchRepo.findByPriceLessThanEqual(maxPrice, pageable);
            }
            return productSearchRepo.findAll(pageable);
        }

        if (minPrice != null || maxPrice != null) {
            Double min = minPrice != null ? minPrice : 0.0;
            Double max = maxPrice != null ? maxPrice : Double.MAX_VALUE;
            return productSearchRepo.searchByQueryAndPriceRange(sanitizedQuery, min, max, pageable);
        }

        return productSearchRepo.searchByQuery(sanitizedQuery, pageable);
    }

    @Override
    public List<String> suggest(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String q = query.trim();
        int size = Math.max(1, Math.min(limit, 50));
        Pageable pageable = PageRequest.of(0, size);
        Page<Product> page = productSearchRepo.customAutocompleteSearch(q, pageable);
        return page.getContent().stream().map(Product::getName).limit(size).toList();
    }

    @Override
    public Product update(Product product) {
        Product updatedProduct = productRepo.save(product);
        // Réindexer dans ElasticSearch
        productSearchRepo.save(updatedProduct);
        return updatedProduct;
    }

    @Override
    public void delete(Product product) {
        productRepo.delete(product);
        // Supprimer de l'index ElasticSearch
        productSearchRepo.delete(product);
    }

    @Override
    public void deleteByUserId(String userId) {
        productRepo.deleteByUserId(userId);
    }
}
