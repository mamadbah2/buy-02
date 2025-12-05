package sn.dev.order_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderResponseDto {
    private String id;
    private String parentOrderId;
    private String sellerId;
    private String userId;
    private Double subTotal;
    private String status;
    private List<OrderItemResponseDto> itemsList;
    private Instant createdAt;
    private Instant updatedAt;
}

