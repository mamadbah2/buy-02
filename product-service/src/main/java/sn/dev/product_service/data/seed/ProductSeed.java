package sn.dev.product_service.data.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.data.repo.ProductRepo;
import sn.dev.product_service.data.repo.elastic.ProductSearchRepo;
import sn.dev.product_service.services.UserServiceClient;
import sn.dev.product_service.web.dto.UserResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ProductSeed implements CommandLineRunner {

    private final ProductRepo productRepo;
    private final UserServiceClient userServiceClient;
    private final ProductSearchRepo productSearchRepo;
    private final Faker faker = new Faker(Locale.FRENCH);

    @Override
    public void run(String... args) {
        // Vider la base de données au démarrage (optionnel)
        if (productRepo.count() == 0) {
            log.info("🧹 La collection de produits est vide. Démarrage du processus de seed...");
            // Vider les index ElasticSearch existants
            productSearchRepo.deleteAll();
            seedProducts();
        }
    }

    private void seedProducts() {
        // Récupérer tous les utilisateurs depuis user-service
        log.info("🔄 Récupération des utilisateurs depuis user-service...");
        List<UserResponse> users;
        try {
            users = userServiceClient.getAllSeller();
            if (users.isEmpty()) {
                log.warn("⚠️ Aucun utilisateur trouvé dans user-service. Veuillez démarrer user-service en premier.");
                return;
            }
            log.info("✅ {} utilisateurs récupérés", users.size());
        } catch (Exception e) {
            log.error("❌ Impossible de récupérer les utilisateurs depuis user-service: {}", e.getMessage());
            log.error("💡 Assurez-vous que user-service est démarré et accessible");
            return;
        }

        List<Product> products = new ArrayList<>();

        // Générer 1500 produits fictifs avec des données variées
        for (int i = 0; i < 1500; i++) {
            Product product = new Product();

            // Catégories variées de produits
            String category = getRandomCategory();
            product.setName(generateProductName(category));
            product.setDescription(faker.lorem().sentence(faker.number().numberBetween(10, 25)));
            product.setPrice(faker.number().randomDouble(2, 5, 10000));
            product.setQuantity(faker.number().numberBetween(0, 1000));

            // Assigner un userId aléatoire parmi les utilisateurs réels
            UserResponse randomUser = users.get(faker.number().numberBetween(0, users.size()));
            product.setUserId(randomUser.getId());

            products.add(product);
        }

        // Sauvegarder tous les produits
        List<Product> savedProducts = productRepo.saveAll(products);
        log.info("✅ {} produits fictifs ont été créés avec succès!", savedProducts.size());

        // Indexer tous les produits dans ElasticSearch
        try {
            log.info("🔍 Indexation des produits dans ElasticSearch...");
            productSearchRepo.saveAll(savedProducts);
            log.info("✅ {} produits indexés dans ElasticSearch avec succès!", savedProducts.size());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'indexation dans ElasticSearch: {}", e.getMessage());
            log.warn("⚠️ Les produits ont été créés dans MongoDB mais pas dans ElasticSearch");
            log.warn("💡 Assurez-vous qu'ElasticSearch est démarré sur {}:{}",
                System.getProperty("elasticsearch.host", "localhost"),
                System.getProperty("elasticsearch.port", "9200"));
        }

        log.info("💡 Les images seront créées automatiquement par media-service au démarrage");
    }

    private void reindexProductsOnElasticSearch() {
        productSearchRepo.deleteAll();
        List<Product> allProducts = productRepo.findAll();
        productSearchRepo.saveAll(allProducts);
    }

    private String getRandomCategory() {
        String[] categories = {
            "electronics", "clothing", "food", "books",
            "furniture", "sports", "beauty", "toys",
            "home", "garden", "automotive", "pets"
        };
        return categories[faker.number().numberBetween(0, categories.length)];
    }

    private String generateProductName(String category) {
        return switch (category) {
            case "electronics" -> faker.options().option(
                faker.device().modelName(),
                faker.app().name() + " " + faker.device().modelName(),
                "Smartphone " + faker.commerce().brand(),
                "Ordinateur " + faker.commerce().brand(),
                "Tablette " + faker.commerce().productName()
            );
            case "clothing" -> faker.options().option(
                faker.color().name() + " " + faker.commerce().productName(),
                "T-shirt " + faker.commerce().brand(),
                "Pantalon " + faker.color().name(),
                "Robe " + faker.commerce().productName(),
                "Veste " + faker.commerce().brand()
            );
            case "food" -> faker.options().option(
                faker.food().fruit(),
                faker.food().vegetable(),
                faker.food().dish(),
                faker.food().ingredient(),
                "Pack " + faker.food().spice()
            );
            case "books" -> faker.book().title() + " - " + faker.book().author();
            case "furniture" -> faker.options().option(
                "Canapé " + faker.commerce().productName(),
                "Table " + faker.color().name(),
                "Chaise " + faker.commerce().brand(),
                "Lit " + faker.commerce().productName(),
                "Armoire " + faker.color().name()
            );
            case "sports" -> faker.options().option(
                faker.esports().game() + " Equipment",
                "Ballon de " + faker.team().sport(),
                "Raquette " + faker.commerce().brand(),
                "Vélo " + faker.commerce().productName(),
                "Équipement " + faker.team().sport()
            );
            case "beauty" -> faker.options().option(
                "Parfum " + faker.commerce().brand(),
                "Crème " + faker.commerce().productName(),
                "Shampooing " + faker.commerce().brand(),
                "Maquillage " + faker.color().name(),
                "Soin " + faker.commerce().productName()
            );
            case "toys" -> faker.options().option(
                faker.superhero().name() + " Figure",
                "Jeu " + faker.commerce().productName(),
                "Puzzle " + faker.color().name(),
                "Peluche " + faker.animal().name(),
                "Jouet " + faker.commerce().brand()
            );
            case "home" -> faker.options().option(
                "Lampe " + faker.color().name(),
                "Coussin " + faker.commerce().productName(),
                "Tapis " + faker.color().name() + " " + faker.commerce().material(),
                "Rideau " + faker.color().name(),
                "Décoration " + faker.commerce().productName()
            );
            case "garden" -> faker.options().option(
                "Plante " + faker.color().name(),
                "Outils " + faker.commerce().productName(),
                "Pot de fleurs " + faker.color().name(),
                "Graines de " + faker.food().vegetable(),
                "Engrais " + faker.commerce().brand()
            );
            case "automotive" -> faker.options().option(
                "Pièce " + faker.vehicle().manufacturer(),
                faker.vehicle().model() + " " + faker.commerce().productName(),
                "Accessoire " + faker.vehicle().manufacturer(),
                "Huile " + faker.commerce().brand(),
                "Pneu " + faker.vehicle().model()
            );
            case "pets" -> faker.options().option(
                "Nourriture pour " + faker.animal().name(),
                "Jouet pour " + faker.animal().name(),
                "Cage " + faker.commerce().productName(),
                "Accessoire " + faker.animal().name(),
                "Laisse " + faker.commerce().brand()
            );
            default -> faker.commerce().productName();
        };
    }
}
