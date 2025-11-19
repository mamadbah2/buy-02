package sn.dev.order_service.services.impl;

import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.data.repository.OrderRepository;
import sn.dev.order_service.services.OrderService;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

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
}
