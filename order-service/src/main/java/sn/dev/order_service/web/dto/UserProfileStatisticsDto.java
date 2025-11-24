package sn.dev.order_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileStatisticsDto {
    private String userId;
    private Double totalSpent;
    private Long totalOrders;
    private List<ProductStatisticsDto> mostPurchasedProducts;
    private List<ProductStatisticsDto> bestSellingProducts;
}


