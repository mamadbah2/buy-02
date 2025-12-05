package sn.dev.order_service.data.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    private String id;
    private String productId;
    private String orderId;
    private String sellerId;
    private Integer quantity;
    private Double unitPrice;

}
