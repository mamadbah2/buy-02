package sn.dev.order_service.services;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.web.dto.OrderItemRequestDto;
import sn.dev.order_service.web.dto.OrderResponseDto;

public interface OrderService {
    Order create(Order order);

    void removeToCart(String orderId, String productId);

    Order updateCart(String id, OrderItem item);

    Order update(Order order);

    Order getById(String id);

    List<Order> getByUserId(String userId);

    List<Order> getAll();

    void delete(Order order);

    Double computeOrdersItems(List<OrderItem> orderItemList);

    void deleteByUserId(String userId);
}
