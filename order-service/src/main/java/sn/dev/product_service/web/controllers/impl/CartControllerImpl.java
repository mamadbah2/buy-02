package sn.dev.product_service.web.controllers.impl;

import org.springframework.http.ResponseEntity;
import sn.dev.product_service.web.controllers.CartController;
import sn.dev.product_service.web.dto.OrderItemRequestDto;
import sn.dev.product_service.web.dto.OrderResponseDto;

public class CartControllerImpl implements CartController {
    @Override
    public ResponseEntity<OrderResponseDto> addToCart(String id, OrderItemRequestDto orderItemRequestDto) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteToCart(String id, String productId) {
        return null;
    }
}
