package sn.dev.order_service.services.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.order_service.client.product.ProductClient;
import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.data.entities.SubOrder;
import sn.dev.order_service.data.repository.OrderRepository;
import sn.dev.order_service.data.repository.SubOrderRepository;
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.dto.ProductResponseDto;
import sn.dev.order_service.web.dto.ProductStatisticsDto;
import sn.dev.order_service.web.dto.UserProfileStatisticsDto;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final ProductClient productClient;

    private static final String NOT_FOUND_MESSAGE = "Order not found with id: ";

    @Override
    public Order create(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public void removeToCart(String orderId, String productId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE + orderId));

        if (order.getOrderItemList() == null || order.getOrderItemList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No items in order: " + orderId);
        }

        boolean removed = order.getOrderItemList().removeIf(item -> productId.equals(item.getProductId()));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in order: " + productId);
        }

        order.setTotal(computeOrdersItems(order.getOrderItemList()));
        orderRepository.save(order);
    }

    @Override
    public Order updateCart(String id, OrderItem item) {
        if (item == null || item.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE + id));

        if (order.getOrderItemList() == null) {
            order.setOrderItemList(new java.util.ArrayList<>());
        }

        List<OrderItem> items = order.getOrderItemList();
        OrderItem existingItem = items.stream()
                .filter(i -> item.getProductId().equals(i.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Mise à jour de l'item existant
            if (item.getQuantity() != null) existingItem.setQuantity(item.getQuantity());
            if (item.getUnitPrice() != null) existingItem.setUnitPrice(item.getUnitPrice());

            // Suppression si quantité <= 0
            if (existingItem.getQuantity() != null && existingItem.getQuantity() <= 0) {
                items.remove(existingItem);
            }
        } else {
            // Ajout d'un nouvel item
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive to add a new item");
            }
            items.add(item);
        }

        order.setTotal(computeOrdersItems(items));
        return orderRepository.save(order);
    }


    @Override
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    public Order getById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE + id));
    }

    @Override
    public List<Order> getByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Order getCartByUserId(String userId) {
        return orderRepository.findByUserIdAndStatus(userId, "CART")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active cart found for user: " + userId));
    }

    @Override
    public Order update(Order order) {
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order confirmOrder(String orderId) {
        // Récupérer la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE + orderId));

        // Vérifier que la commande n'a pas déjà été divisée
        if (Boolean.TRUE.equals(order.getIsSplit())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Order has already been split into sub-orders");
        }

        // Vérifier que la commande a des items
        if (order.getOrderItemList() == null || order.getOrderItemList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot confirm an order with no items");
        }

        // Enrichir les items avec le sellerId si nécessaire
        enrichOrderItemsWithSellerId(order.getOrderItemList());

        // Grouper les items par sellerId
        Map<String, List<OrderItem>> itemsBySeller = order.getOrderItemList().stream()
                .filter(item -> item.getSellerId() != null)
                .collect(Collectors.groupingBy(OrderItem::getSellerId));

        // Créer une SubOrder pour chaque vendeur
        List<SubOrder> subOrders = itemsBySeller.entrySet().stream()
                .map(entry -> {
                    String sellerId = entry.getKey();
                    List<OrderItem> sellerItems = entry.getValue();

                    // Calculer le sous-total pour ce vendeur
                    Double subTotal = sellerItems.stream()
                            .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                            .sum();

                    // Créer la SubOrder
                    return new SubOrder(
                            order.getId(),
                            sellerId,
                            order.getUserId(),
                            subTotal,
                            "PENDING",
                            sellerItems
                    );
                })
                .toList();

        // Sauvegarder toutes les SubOrders
        subOrderRepository.saveAll(subOrders);

        // Mettre à jour le statut de la commande principale
        order.setStatus("PENDING");
        order.setIsSplit(true);

        return orderRepository.save(order);
    }

    @Override
    public List<SubOrder> getSubOrdersByParentOrderId(String parentOrderId) {
        return subOrderRepository.findByParentOrderId(parentOrderId);
    }

    /**
     * Enrichit les OrderItems avec le sellerId en interrogeant le ProductClient
     */
    private void enrichOrderItemsWithSellerId(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            if (item.getSellerId() == null) {
                try {
                    ProductResponseDto product = productClient.getById(item.getProductId());
                    item.setSellerId(product.getUserId());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Unable to fetch seller information for product: " + item.getProductId(), e);
                }
            }
        }
    }

    @Override
    public void delete(Order order) {
        orderRepository.delete(order);
    }

    @Override
    public Double computeOrdersItems(List<OrderItem> orderItemList) {
        if (orderItemList == null) return 0.;
        return orderItemList.stream()
                .mapToDouble(orderItem -> orderItem.getQuantity() * orderItem.getUnitPrice())
                .sum();
    }

    @Override
    public void deleteByUserId(String userId) {
        orderRepository.deleteByUserId(userId);
    }

    @Override
    public UserProfileStatisticsDto getUserStatistics(String userId) {
        // Get all orders for the user (excluding CART status)
        List<Order> userOrders = orderRepository.findByUserId(userId).stream()
                .filter(order -> !"CART".equals(order.getStatus()))
                .toList();

        // Calculate total spent and total orders
        double totalSpent = userOrders.stream()
                .mapToDouble(Order::getTotal)
                .sum();
        long totalOrders = userOrders.size();

        // Aggregate product statistics
        Map<String, ProductStatisticsDto> productStatsMap = userOrders.stream()
                .flatMap(order -> order.getOrderItemList().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    String productId = items.get(0).getProductId();
                                    int totalQuantity = items.stream()
                                            .mapToInt(OrderItem::getQuantity)
                                            .sum();
                                    double totalRevenue = items.stream()
                                            .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                                            .sum();
                                    long orderCount = items.stream()
                                            .map(OrderItem::getOrderId)
                                            .distinct()
                                            .count();

                                    // Fetch product details
                                    String productName = productId;
                                    try {
                                        ProductResponseDto product = productClient.getById(productId);
                                        productName = product.getName();
                                    } catch (Exception e) {
                                        // If product not found, use productId as name
                                    }

                                    return new ProductStatisticsDto(
                                            productId,
                                            productName,
                                            totalQuantity,
                                            totalRevenue,
                                            orderCount
                                    );
                                }
                        )
                ));

        // Sort by quantity for most purchased products (top 5)
        List<ProductStatisticsDto> mostPurchasedProducts = productStatsMap.values().stream()
                .sorted((p1, p2) -> p2.getTotalQuantity().compareTo(p1.getTotalQuantity()))
                .limit(5)
                .toList();

        // Sort by revenue for best selling products (top 5)
        List<ProductStatisticsDto> bestSellingProducts = productStatsMap.values().stream()
                .sorted((p1, p2) -> p2.getTotalRevenue().compareTo(p1.getTotalRevenue()))
                .limit(5)
                .toList();

        return new UserProfileStatisticsDto(
                userId,
                totalSpent,
                totalOrders,
                mostPurchasedProducts,
                bestSellingProducts
        );
    }
}
