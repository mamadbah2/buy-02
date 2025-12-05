package sn.dev.order_service.data.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "order")
public class Order {
    @Id
    private String id;
    private String userId;
    private Double total;
    private String status;
    private String paymentMethod;
    private List<OrderItem> orderItemList;
    private Instant createdAt;
    private Boolean isSplit = false;

    public Order(String userId, Double total, String status, String paymentMethod) {
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.createdAt = Instant.now();
        this.isSplit = false;
    }

}

