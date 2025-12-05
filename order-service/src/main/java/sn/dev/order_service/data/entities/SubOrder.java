package sn.dev.order_service.data.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sub_order")
public class SubOrder {
    @Id
    private String id;
    private String parentOrderId;
    private String sellerId;
    private String userId;
    private Double subTotal;
    private String status;
    private List<OrderItem> itemsList;
    private Instant createdAt;
    private Instant updatedAt;

    public SubOrder(String parentOrderId, String sellerId, String userId, Double subTotal, String status, List<OrderItem> itemsList) {
        this.parentOrderId = parentOrderId;
        this.sellerId = sellerId;
        this.userId = userId;
        this.subTotal = subTotal;
        this.status = status;
        this.itemsList = itemsList;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}

