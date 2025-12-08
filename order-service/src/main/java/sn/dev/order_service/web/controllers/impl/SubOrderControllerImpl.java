package sn.dev.order_service.web.controllers.impl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import sn.dev.order_service.data.entities.SubOrder;
import sn.dev.order_service.services.SubOrderService;
import sn.dev.order_service.web.controllers.SubOrderController;
import sn.dev.order_service.web.dto.SubOrderResponseDto;
import sn.dev.order_service.web.dto.SubOrderStatusUpdateDto;
import sn.dev.order_service.web.mappers.SubOrderMapper;


@Slf4j
@RestController
@RequiredArgsConstructor
public class SubOrderControllerImpl implements SubOrderController {
    private final SubOrderService subOrderService;
    private final SubOrderMapper subOrderMapper;

    @Override
    public ResponseEntity<Page<SubOrderResponseDto>> getSubOrdersBySellerId(
            String sellerId,
            String status,
            int page,
            int size
    ) {
        log.info("GET sub-orders for seller: {}, status: {}, page: {}, size: {}",
                sellerId, status, page, size);

        // verifie si c'est bien lui le seller qui demande les sub-orders
        // Connected User
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString("userID");

        if (!sellerId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        // Créer le Pageable
        Pageable pageable = PageRequest.of(page, size);

        // Récupérer les sub-orders paginées
        Page<SubOrder> subOrdersPage = subOrderService.getSubOrdersBySeller(
                sellerId,
                status,
                pageable
        );

        if (subOrdersPage.getContent().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Convertir en DTO
        Page<SubOrderResponseDto> responsePage = subOrdersPage.map(subOrderMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<SubOrderResponseDto> updateSubOrderStatus(
            String id,
            @Valid SubOrderStatusUpdateDto statusUpdateDto
    ) {
        log.info("UPDATE sub-order status: id={}, newStatus={}", id, statusUpdateDto.getStatus());

        SubOrder subOrder = subOrderService.getById(id);

        // verifie si c'est bien lui le seller qui demande les sub-orders
        // Connected User
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString("userID");

        if (!subOrder.getSellerId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        // Mettre à jour le status via le service
        SubOrder updatedSubOrder = subOrderService.updateStatus(id, statusUpdateDto.getStatus());

        // Convertir en DTO
        SubOrderResponseDto responseDto = subOrderMapper.toResponse(updatedSubOrder);

        return ResponseEntity.ok(responseDto);
    }
}
