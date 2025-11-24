package sn.dev.order_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatisticsDto {
    private String productId;
    private String productName;
    private Integer totalQuantity;
    private Double totalRevenue;
    private Long orderCount;
}