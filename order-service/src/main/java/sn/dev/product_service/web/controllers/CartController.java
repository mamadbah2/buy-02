package sn.dev.product_service.web.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.dev.product_service.web.dto.OrderItemRequestDto;
import sn.dev.product_service.web.dto.OrderResponseDto;




@RestController("/api/cart")
public interface CartController {
    @PatchMapping("/{id}")
    ResponseEntity<OrderResponseDto> addToCart(@PathVariable String id, @RequestBody @Valid OrderItemRequestDto orderItemRequestDto);

    @DeleteMapping("/{id}/products/{productId}")
    ResponseEntity<Void> deleteToCart(@PathVariable String id, @PathVariable String productId);

}
