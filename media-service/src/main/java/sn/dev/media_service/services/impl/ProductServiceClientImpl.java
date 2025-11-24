package sn.dev.media_service.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import sn.dev.media_service.services.ProductServiceClient;
import sn.dev.media_service.web.controllers.dto.PageResponse;
import sn.dev.media_service.web.controllers.dto.ProductDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceClientImpl implements ProductServiceClient {

    private final Faker faker = new Faker(Locale.FRENCH);

    @Override
    public List<ProductDto> fetchProductsFromProductService() {
        List<ProductDto> allProducts = new ArrayList<>();

        try {
            String productServiceUrl = System.getenv().getOrDefault("PRODUCT_SERVICE_URL", "http://localhost:8082");
            RestTemplate restTemplate = new RestTemplate();

            int currentPage = 0;
            int totalPages;
            int pageSize = 300; // Récupérer 100 produits par page pour optimiser

            do {
                String url = String.format("%s/api/products?page=%d&size=%d",
                                           productServiceUrl, currentPage, pageSize);

                ResponseEntity<PageResponse<ProductDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<PageResponse<ProductDto>>() {}
                );

                PageResponse<ProductDto> pageResponse = response.getBody();

                if (pageResponse != null && pageResponse.getContent() != null) {
                    allProducts.addAll(pageResponse.getContent());
                    totalPages = pageResponse.getTotalPages();

                    log.info("📦 Page {}/{} récupérée: {} produits",
                             currentPage + 1, totalPages, pageResponse.getContent().size());

                    currentPage++;
                } else {
                    break;
                }

            } while (currentPage < totalPages);

            log.info("✅ Total: {} produits récupérés depuis product-service", allProducts.size());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des produits: {}", e.getMessage(), e);
        }

        return allProducts;
    }

    @Override
    public String inferCategoryFromProductName(String name) {
        if (name == null) return "general";

        String lowerName = name.toLowerCase();

        if (lowerName.contains("smartphone") || lowerName.contains("ordinateur") ||
            lowerName.contains("tablette") || lowerName.contains("electronic")) {
            return "electronics";
        } else if (lowerName.contains("shirt") || lowerName.contains("pantalon") ||
                   lowerName.contains("robe") || lowerName.contains("veste")) {
            return "clothing";
        } else if (lowerName.contains("canapé") || lowerName.contains("table") ||
                   lowerName.contains("chaise") || lowerName.contains("lit")) {
            return "furniture";
        } else if (lowerName.contains("ballon") || lowerName.contains("vélo") ||
                   lowerName.contains("raquette")) {
            return "sports";
        } else if (lowerName.contains("parfum") || lowerName.contains("crème") ||
                   lowerName.contains("shampooing")) {
            return "beauty";
        } else if (lowerName.contains("jouet") || lowerName.contains("peluche") ||
                   lowerName.contains("puzzle")) {
            return "toys";
        }

        return "general";
    }

    @Override
    public String getRandomCategory() {
        String[] categories = {
            "electronics", "clothing", "food", "books",
            "furniture", "sports", "beauty", "toys",
            "home", "garden", "automotive", "pets"
        };
        return categories[faker.number().numberBetween(0, categories.length)];
    }
}
