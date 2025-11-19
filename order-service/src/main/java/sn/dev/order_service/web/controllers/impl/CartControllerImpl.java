package sn.dev.order_service.web.controllers.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.controllers.CartController;
import sn.dev.order_service.web.dto.OrderItemRequestDto;
import sn.dev.order_service.web.dto.OrderResponseDto;
import sn.dev.order_service.web.mappers.OrdersItemsMappers;
import sn.dev.order_service.web.mappers.OrdersMappers;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CartControllerImpl implements CartController {
    private final OrderService orderService;
    private final OrdersItemsMappers ordersItemsMappers;
    private final OrdersMappers ordersMappers;

    // Get the cart of the current user
    @Override
    public ResponseEntity<OrderResponseDto> getUserCart(String id) {
        log.info("GET cart for user: {}", id);
        Order order = orderService.getCartByUserId(id);
        OrderResponseDto response = ordersMappers.toResponse(order);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<OrderResponseDto> updateCart(String id, OrderItemRequestDto orderItemRequestDto) {
        log.info("UPDATE cart item in order: {}", id);
        OrderItem item = ordersItemsMappers.toEntity(orderItemRequestDto);
        Order updated = orderService.updateCart(id, item);
        return ResponseEntity.ok(ordersMappers.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteToCart(String id, String productId) {
        orderService.removeToCart(id, productId);
        return ResponseEntity.noContent().build();
    }
}
