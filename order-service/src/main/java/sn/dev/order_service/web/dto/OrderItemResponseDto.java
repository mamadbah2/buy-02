package sn.dev.order_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sn.dev.order_service.data.entities.OrderItem;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
    private String productId;
    private Integer quantity;
    private Double price;

    public OrderItemResponseDto(OrderItem orderItem) {
        this.price = orderItem.getUnitPrice();
        this.quantity = orderItem.getQuantity();
        this.productId = orderItem.getProductId();
    }
}
