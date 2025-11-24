package sn.dev.order_service.web.dto;

import java.time.Instant;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderResponseDto {
    private String id;
    private String userId;
    private Double total;
    private String status;
    private String paymentMethod;
    private Instant createdAt;
    private List<OrderItemResponseDto> items;
}
