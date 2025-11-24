package sn.dev.media_service.data.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import sn.dev.media_service.data.entities.Media;
import sn.dev.media_service.data.repos.MediaRepo;
import sn.dev.media_service.services.ProductServiceClient;
import sn.dev.media_service.web.controllers.dto.ProductDto;

import java.util.*;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class MediaSeeding implements CommandLineRunner {

    private final MediaRepo mediaRepo;
    private final ProductServiceClient productServiceClient;
    private final Faker faker = new Faker(Locale.FRENCH);

    @Override
    public void run(String... args) {
        if (mediaRepo.count() == 0) {
            seedMedia();
        }
    }

    private void seedMedia() {
        // Récupérer tous les produits depuis product-service
        List<ProductDto> products = productServiceClient.fetchProductsFromProductService();

        if (products.isEmpty()) {
            log.warn("⚠️ Aucun produit trouvé dans product-service. Création de médias factices...");
            return;
        }

        List<Media> allMedia = new ArrayList<>();

        // Générer 2-5 images par produit
        for (ProductDto product : products) {
            String category = productServiceClient.inferCategoryFromProductName(product.getName());
            int imageCount = faker.number().numberBetween(2, 6);

            for (int i = 0; i < imageCount; i++) {
                Media media = new Media();
                media.setImageUrl(generateImageUrl(category, i));
                media.setProductId(product.getId());
                allMedia.add(media);
            }
        }

        mediaRepo.saveAll(allMedia);
        log.info("✅ {} images fictives ont été créées pour {} produits!", allMedia.size(), products.size());
    }

    private String generateImageUrl(String category, int index) {
        // Utiliser Picsum Photos avec des seeds basés sur la catégorie
        int seed = category.hashCode() + index * 123;
        int width = 800;
        int height = 600;

        // Variante: parfois format carré, parfois portrait
        if (index % 3 == 0) {
            width = height = 600; // Carré
        } else if (index % 3 == 1) {
            width = 600;
            height = 800; // Portrait
        }

        return String.format("https://picsum.photos/seed/%d/%d/%d", Math.abs(seed), width, height);
    }

}
