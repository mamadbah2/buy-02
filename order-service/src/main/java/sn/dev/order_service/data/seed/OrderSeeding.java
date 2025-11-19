package sn.dev.order_service.data.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import sn.dev.order_service.client.product.ProductClient;
import sn.dev.order_service.client.user.UserClient;
import sn.dev.order_service.web.dto.UserResponseDto;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.data.repository.OrderRepository;
import sn.dev.order_service.web.dto.ProductResponseDto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class OrderSeeding implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final Faker faker = new Faker(Locale.FRENCH);

    // Nombre de commandes à générer
    private static final int NUMBER_OF_ORDERS = 1000;

    // Statuts possibles de commande
    private static final String[] ORDER_STATUSES = {
           "CART", "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"
    };

    // Méthodes de paiement possibles
    private static final String[] PAYMENT_METHODS = {
            "DEBIT_CARD", "PAYPAL", "WAVE", "CASH_ON_DELIVERY", "ORANGE_MONEY"
    };

    @Override
    public void run(String... args) {
        if (orderRepository.count() == 0) {
            log.info("🚀 Début du seeding des commandes...");
            seedOrders();
            log.info("✅ Seeding des commandes terminé avec succès!");
        } else {
            log.info("ℹ️ Les commandes existent déjà, seeding ignoré.");
        }
    }

    private void seedOrders() {
        try {
            // Récupérer tous les utilisateurs
            List<UserResponseDto> users = fetchUsers();
            if (users.isEmpty()) {
                log.warn("⚠️ Aucun utilisateur trouvé. Impossible de créer des commandes.");
                return;
            }

            // Récupérer tous les produits
            List<ProductResponseDto> products = fetchProducts();
            if (products.isEmpty()) {
                log.warn("⚠️ Aucun produit trouvé. Impossible de créer des commandes.");
                return;
            }

            log.info("📦 {} utilisateurs et {} produits récupérés", users.size(), products.size());
            log.info("🛒 Génération de {} commandes...", NUMBER_OF_ORDERS);

            List<sn.dev.order_service.data.entities.Order> orders = new ArrayList<>();

            for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
                // Sélectionner un utilisateur aléatoire (uniquement les clients)
                List<UserResponseDto> clients = users.stream()
                        .filter(u -> "CLIENT".equals(u.getRole()))
                        .toList();

                if (clients.isEmpty()) {
                    clients = users; // Si pas de clients, utiliser tous les users
                }

                UserResponseDto randomUser = clients.get(faker.number().numberBetween(0, clients.size()));

                // Créer une commande avec 1-5 items
                sn.dev.order_service.data.entities.Order order = createOrder(randomUser, products);
                orders.add(order);
            }

            // Sauvegarder toutes les commandes
            orderRepository.saveAll(orders);

            log.info("✅ {} commandes créées avec succès!", orders.size());
            logOrderStatistics(orders);

        } catch (Exception e) {
            log.error("❌ Erreur lors du seeding des commandes: {}", e.getMessage());
            log.info("🔄 Création de commandes factices...");
        }
    }

    private List<UserResponseDto> fetchUsers() {
        try {
            List<UserResponseDto> users = userClient.getAllUsers();
            log.info("👥 {} utilisateurs récupérés depuis user-service", users.size());
            return users;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des utilisateurs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ProductResponseDto> fetchProducts() {
        List<ProductResponseDto> allProducts = new ArrayList<>();

        try {
            int currentPage = 0;
            int totalPages;
            int pageSize = 200; // Récupérer 100 produits par page

            do {
                log.info("📦 Récupération de la page {} de produits...", currentPage + 1);

                sn.dev.order_service.web.dto.PageResponse<ProductResponseDto> pageResponse =
                    productClient.getProductsPage(currentPage, pageSize);

                if (pageResponse != null && pageResponse.getContent() != null) {
                    allProducts.addAll(pageResponse.getContent());
                    totalPages = pageResponse.getTotalPages();

                    log.info("✅ Page {}/{} récupérée: {} produits",
                             currentPage + 1, totalPages, pageResponse.getContent().size());

                    currentPage++;

                    // Sécurité: arrêter après 100 pages maximum
                    if (currentPage >= 100) {
                        log.warn("⚠️ Limite de 100 pages atteinte, arrêt de la récupération");
                        break;
                    }
                } else {
                    log.warn("⚠️ Réponse vide à la page {}", currentPage);
                    break;
                }

            } while (currentPage < totalPages);

            log.info("📦 {} produits récupérés au total depuis product-service", allProducts.size());
            return allProducts;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des produits: {}", e.getMessage());
            log.debug("Détails de l'erreur:", e);
            return new ArrayList<>();
        }
    }

    private sn.dev.order_service.data.entities.Order createOrder(UserResponseDto user, List<ProductResponseDto> products) {
        sn.dev.order_service.data.entities.Order order = new sn.dev.order_service.data.entities.Order();

        order.setUserId(user.getId());
        order.setStatus(getRandomStatus());
        order.setPaymentMethod(getRandomPaymentMethod());
        order.setCreatedAt(getRandomPastDate());

        // Créer 1-5 items pour cette commande
        int itemCount = faker.number().numberBetween(1, 6);
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0.0;

        for (int i = 0; i < itemCount; i++) {
            OrderItem item = createOrderItem(products);
            orderItems.add(item);
            total += item.getUnitPrice() * item.getQuantity();
        }

        order.setOrderItemList(orderItems);
        order.setTotal(Math.round(total * 100.0) / 100.0); // Arrondir à 2 décimales

        return order;
    }

    private OrderItem createOrderItem(List<ProductResponseDto> products) {
        OrderItem item = new OrderItem();

        // Si on a des produits réels, utiliser un ID aléatoire, sinon générer

        ProductResponseDto randomProduct = products.get(faker.number().numberBetween(0, products.size()));
        item.setProductId(randomProduct.getId());
        item.setUnitPrice(randomProduct.getPrice());


        item.setQuantity(faker.number().numberBetween(1, 10));

        return item;
    }

    private String getRandomStatus() {
        return ORDER_STATUSES[faker.number().numberBetween(0, ORDER_STATUSES.length)];
    }

    private String getRandomPaymentMethod() {
        return PAYMENT_METHODS[faker.number().numberBetween(0, PAYMENT_METHODS.length)];
    }

    private Instant getRandomPastDate() {
        // Générer une date entre 365 jours et aujourd'hui
        int daysAgo = faker.number().numberBetween(1, 366);
        return Instant.now().minus(daysAgo, ChronoUnit.DAYS);
    }

    private void logOrderStatistics(List<sn.dev.order_service.data.entities.Order> orders) {
        // Statistiques par statut
        log.info("📊 Statistiques des commandes:");
        for (String status : ORDER_STATUSES) {
            long count = orders.stream().filter(o -> status.equals(o.getStatus())).count();
            if (count > 0) {
                log.info("   - {}: {}", status, count);
            }
        }

        // Statistiques par méthode de paiement
        log.info("💳 Méthodes de paiement:");
        for (String method : PAYMENT_METHODS) {
            long count = orders.stream().filter(o -> method.equals(o.getPaymentMethod())).count();
            if (count > 0) {
                log.info("   - {}: {}", method, count);
            }
        }

        // Total général
        double totalRevenue = orders.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .mapToDouble(sn.dev.order_service.data.entities.Order::getTotal)
                .sum();
        log.info("💰 Revenu total (hors annulations): {} €", String.format("%.2f", totalRevenue));
    }
}
