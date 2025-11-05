package sn.dev.product_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestDto {
    @NotBlank(message = "ProductId cannot be blank")
    private String productId;
    @NotBlank(message = "Quantity of Product cannot be blank")
    private Integer quantity;
}
