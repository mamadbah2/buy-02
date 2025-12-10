package sn.dev.order_service.web.controllers.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.controllers.CartController;
import sn.dev.order_service.web.dto.OrderItemPatchDto;
import sn.dev.order_service.web.dto.OrderResponseDto;
import sn.dev.order_service.web.mappers.OrdersItemsMappers;
import sn.dev.order_service.web.mappers.OrdersMappers;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CartControllerImpl implements CartController {
    private final OrderService orderService;
    private final OrdersItemsMappers ordersItemsMappers;
    private final OrdersMappers ordersMappers;

    // Get the cart of the current user
    @Override
    public ResponseEntity<OrderResponseDto> getUserCart(String id) {
        log.info("GET cart for user: {}", id);

        Order order = orderService.getCartByUserId(id);

        // control if the user is the right one
        // Connected User
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString("userID");

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to update this order"
            );
        }

        OrderResponseDto response = ordersMappers.toResponse(order);
        return ResponseEntity.ok(response);
    }

    @Override
    // Ajoute un produit dans le panier de l'utilisateur ou update la quantite si ce dernier existe'
    public ResponseEntity<OrderResponseDto> updateCart(String id, OrderItemPatchDto orderItemPatchDto) {
        // Récupère l'id du produit depuis le DTO
        final String productId = orderItemPatchDto.getProductId();
        log.info("UPDATE cart item in order: {} for product: {}", id, productId);

        // Construit l'OrderItem (avec récupération du prix côté ProductClient)
        OrderItem item = ordersItemsMappers.toEntity(orderItemPatchDto, productId);

        // Le service gère : ajout si le produit n'existe pas encore, sinon mise à jour de la quantité
        Order updated = orderService.updateCart(id, item);
        return ResponseEntity.ok(ordersMappers.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteToCart(String id, String productId) {
        log.info("DELETE cart item in order: {} for product: {}", id, productId);
        orderService.removeToCart(id, productId);
        return ResponseEntity.noContent().build();
    }
}
