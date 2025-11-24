package sn.dev.order_service.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemPatchDto {
    @NotBlank(message = "Value of Product ID cannot be blank")
    private String productId;
    @NotNull(message = "Quantity of Product cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}

