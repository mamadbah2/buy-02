package sn.dev.product_service.services;

import java.util.List;

import sn.dev.product_service.data.entities.Order;
import sn.dev.product_service.data.entities.OrderItem;

public interface OrderService {
    Order create(Order order);

    Order addToCart(String orderId, OrderItem item);

    void removeToCart(String orderId, String productId);

    Order update(Order order);

    Order getById(String id);

    List<Order> getByUserId(String userId);

    List<Order> getAll();

    void delete(Order order);

    Double computeOrdersItems(List<OrderItem> orderItemList);

    void deleteByUserId(String userId);
}
