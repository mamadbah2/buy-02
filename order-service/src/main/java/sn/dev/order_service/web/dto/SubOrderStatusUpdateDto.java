package sn.dev.order_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderStatusUpdateDto {
    @NotBlank(message = "Status is required")
    private String status;
}

