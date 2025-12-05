package sn.dev.order_service.web.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import sn.dev.order_service.web.dto.*;

@RequestMapping("/api/orders")
public interface OrderController {
    @PostMapping
    ResponseEntity<OrderResponseDto> create(@RequestBody @Valid OrderRequestDto orderRequestDto);

    @GetMapping
    ResponseEntity<List<OrderResponseDto>> getAll();

    @GetMapping("/user/{userId}")
    ResponseEntity<List<OrderResponseDto>> getByUserId(@PathVariable String userId);

    @GetMapping("/{id}")
    ResponseEntity<OrderResponseDto> getById(@PathVariable String id);

    @PatchMapping("/{id}/command")
    ResponseEntity<OrderResponseDto> update(@RequestBody OrderPatchDto orderPatchDto,
                                            @PathVariable String id);

    @PostMapping("/{id}/confirm")
    ResponseEntity<OrderResponseDto> confirmOrder(@PathVariable String id);

    @GetMapping("/{id}/sub-orders")
    ResponseEntity<List<SubOrderResponseDto>> getSubOrders(@PathVariable String id);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id);

    @GetMapping("/statistics/user/{clientId}")
    ResponseEntity<UserProfileStatisticsDto> getUserStatistics(@PathVariable String clientId);
}
