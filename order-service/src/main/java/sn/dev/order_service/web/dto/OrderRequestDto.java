package sn.dev.order_service.web.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class OrderRequestDto {
    @NotBlank(message = "userId cannot be blank")
    private String userId;

    private String paymentMethod;
    @NotBlank(message = "Status cannot be blank")
    private String status;
    @NotNull(message = "At least one item is required")
    private List<OrderItemRequestDto> items; // produits commandés
}

