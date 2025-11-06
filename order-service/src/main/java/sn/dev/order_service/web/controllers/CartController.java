package sn.dev.order_service.web.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.dev.order_service.web.dto.OrderItemRequestDto;
import sn.dev.order_service.web.dto.OrderResponseDto;

@RequestMapping("/api/cart")
public interface CartController {
    @GetMapping
    ResponseEntity<String> testCart();

    @PatchMapping("/{id}/products/{productId}")
    ResponseEntity<OrderResponseDto> updateCart(@PathVariable String id, @RequestBody @Valid OrderItemRequestDto orderItemRequestDto);

    @DeleteMapping("/{id}/products/{productId}")
    ResponseEntity<Void> deleteToCart(@PathVariable String id, @PathVariable String productId);
}
