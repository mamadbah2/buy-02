package sn.dev.order_service.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.dev.order_service.web.dto.OrderItemPatchDto;
import sn.dev.order_service.web.dto.OrderResponseDto;

@RequestMapping("/api/cart")
public interface CartController {
    @GetMapping("user/{id}")
    ResponseEntity<OrderResponseDto> getUserCart(@PathVariable String id);

    @PatchMapping("/{id}")
    ResponseEntity<OrderResponseDto> updateCart(@PathVariable String id, @RequestBody OrderItemPatchDto orderItemPatchDto);

    @DeleteMapping("/{id}/products/{productId}")
    ResponseEntity<Void> deleteToCart(@PathVariable String id, @PathVariable String productId);
}
