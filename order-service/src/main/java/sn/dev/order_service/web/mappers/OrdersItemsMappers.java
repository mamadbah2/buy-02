package sn.dev.order_service.web.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.dev.order_service.client.product.ProductClient;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.web.dto.OrderItemRequestDto;
import sn.dev.order_service.web.dto.OrderItemPatchDto;
import sn.dev.order_service.web.dto.ProductResponseDto;

@RequiredArgsConstructor
@Component
public class OrdersItemsMappers {
    private final ProductClient productClient;

    public OrderItem toEntity(OrderItemRequestDto orderItemRequestDto) {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(orderItemRequestDto.getQuantity());

        var productId = orderItemRequestDto.getProductId();
        orderItem.setProductId(productId);
        ProductResponseDto productResponseDto = productClient.getById(productId);
        orderItem.setUnitPrice(productResponseDto.getPrice());

        return orderItem;
    }

    public OrderItem toEntity(OrderItemPatchDto orderItemPatchDto, String productId) {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(orderItemPatchDto.getQuantity());
        orderItem.setProductId(productId);
        ProductResponseDto productResponseDto = productClient.getById(productId);
        orderItem.setUnitPrice(productResponseDto.getPrice());

        return orderItem;
    }
}
