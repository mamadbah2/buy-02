package sn.dev.order_service.web.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.dev.order_service.data.entities.SubOrder;
import sn.dev.order_service.web.dto.OrderItemResponseDto;
import sn.dev.order_service.web.dto.SubOrderResponseDto;

import java.util.List;

@RequiredArgsConstructor
@Component
public class SubOrderMapper {

    public SubOrderResponseDto toResponse(SubOrder subOrder) {
        SubOrderResponseDto dto = new SubOrderResponseDto();
        dto.setId(subOrder.getId());
        dto.setParentOrderId(subOrder.getParentOrderId());
        dto.setSellerId(subOrder.getSellerId());
        dto.setUserId(subOrder.getUserId());
        dto.setSubTotal(subOrder.getSubTotal());
        dto.setStatus(subOrder.getStatus());
        dto.setCreatedAt(subOrder.getCreatedAt());
        dto.setUpdatedAt(subOrder.getUpdatedAt());
        
        List<OrderItemResponseDto> itemsDtoList = subOrder.getItemsList().stream()
                .map(OrderItemResponseDto::new)
                .toList();
        dto.setItemsList(itemsDtoList);
        
        return dto;
    }
}

