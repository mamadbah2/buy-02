package sn.dev.order_service.services;

import java.util.List;

import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;

public interface OrderService {
    Order create(Order order);

    void removeToCart(String orderId, String productId);

    Order updateCart(String id, OrderItem item);

    Order update(Order order);

    Order getById(String id);

    List<Order> getByUserId(String userId);

    Order getCartByUserId(String userId);

    List<Order> getAll();

    void delete(Order order);

    Double computeOrdersItems(List<OrderItem> orderItemList);

    void deleteByUserId(String userId);
}
