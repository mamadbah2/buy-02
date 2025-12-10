package sn.dev.order_service.web.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.dev.order_service.web.dto.SubOrderResponseDto;
import sn.dev.order_service.web.dto.SubOrderStatusUpdateDto;

@RequestMapping("/api/sub-orders")
public interface SubOrderController {

    @GetMapping("/seller/{sellerId}")
    ResponseEntity<Page<SubOrderResponseDto>> getSubOrdersBySellerId(
            @PathVariable String sellerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @PatchMapping("/{id}/status")
    ResponseEntity<SubOrderResponseDto> updateSubOrderStatus(
            @PathVariable String id,
            @RequestBody @Valid SubOrderStatusUpdateDto statusUpdateDto
    );
}
