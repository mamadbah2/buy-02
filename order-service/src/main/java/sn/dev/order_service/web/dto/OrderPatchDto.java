package sn.dev.order_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderPatchDto {
    @NotBlank(message = "Status cannot be blank")
    private String status;
    @NotBlank(message = "Status cannot be blank")
    private String paymentMethod;
}

