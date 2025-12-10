package sn.dev.order_service.web.controllers.impl;

import jakarta.validation.Valid;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.SubOrder;
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.controllers.OrderController;
import sn.dev.order_service.web.dto.OrderRequestDto;
import sn.dev.order_service.web.dto.OrderPatchDto;
import sn.dev.order_service.web.dto.OrderResponseDto;
import sn.dev.order_service.web.dto.SubOrderResponseDto;
import sn.dev.order_service.web.dto.UserProfileStatisticsDto;
import sn.dev.order_service.web.mappers.OrdersMappers;
import sn.dev.order_service.web.mappers.SubOrderMapper;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;
    private final OrdersMappers ordersMappers;
    private final SubOrderMapper subOrderMapper;
    private static final String MAX_AGE = "300";
    private static final String USERIDSTR = "userID";

    @Override
    public ResponseEntity<OrderResponseDto> create(
        @Valid OrderRequestDto orderRequestDto
    ) {
        log.info("CREATE order : {}", orderRequestDto);

        Order order = orderService.create(
            ordersMappers.toEntity(orderRequestDto)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + MAX_AGE)
            .body(ordersMappers.toResponse(order));
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getAll() {
        log.info("GET(getAll) orders");

        List<Order> orders = orderService.getAll();
        List<OrderResponseDto> responseList = orders
            .stream()
            .map(ordersMappers::toResponse)
            .toList();

        return ResponseEntity.ok(responseList);
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getByUserId(String userId) {
        log.info("GET orders by userId: {}", userId);

        List<Order> orders = orderService.getByUserId(userId);
        List<OrderResponseDto> responseList = orders
            .stream()
            .map(ordersMappers::toResponse)
            .toList();

        return ResponseEntity.ok(responseList);
    }

    @Override
    public ResponseEntity<OrderResponseDto> getById(String id) {
        log.info("GET(order by id) order with id: {}", id);

        Order order = orderService.getById(id);

        return ResponseEntity.ok(ordersMappers.toResponse(order));
    }

    @Override
    public ResponseEntity<OrderResponseDto> update(
        @Valid OrderPatchDto orderPatchDto,
        String id
    ) {
        log.info("UPDATE(order by id) order with id: {}", id);
        Order order = orderService.getById(id);

        // Connected User
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to update this order"
            );
        }

        // Update only the status and payment field for PATCH operation
        order.setStatus(orderPatchDto.getStatus());
        order.setPaymentMethod(orderPatchDto.getPaymentMethod());

        Order updatedOrder = orderService.update(order);

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + MAX_AGE)
            .body(ordersMappers.toResponse(updatedOrder));
    }

    @Override
    public ResponseEntity<OrderResponseDto> confirmOrder(String id) {
        log.info("CONFIRM order with id: {}", id);

        // Connected User
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        // Get order and verify ownership
        Order order = orderService.getById(id);
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to confirm this order"
            );
        }

        // Confirm order and trigger order splitting
        Order confirmedOrder = orderService.confirmOrder(id);

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + MAX_AGE)
            .body(ordersMappers.toResponse(confirmedOrder));
    }

    @Override
    public ResponseEntity<List<SubOrderResponseDto>> getSubOrders(String id) {
        log.info("GET sub-orders for order id: {}", id);

        // Connected User
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        // Verify order ownership
        Order order = orderService.getById(id);
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to view sub-orders for this order"
            );
        }

        // Get sub-orders
        List<SubOrder> subOrders = orderService.getSubOrdersByParentOrderId(id);
        List<SubOrderResponseDto> responseList = subOrders
            .stream()
            .map(subOrderMapper::toResponse)
            .toList();

        return ResponseEntity.ok(responseList);
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        log.info("DELETE(order by id) order with id: {}", id);
        Order order = orderService.getById(id);

        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString(USERIDSTR);

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to delete this order"
            );
        }

        orderService.delete(order);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserProfileStatisticsDto> getUserStatistics(String clientId) {
        log.info("GET user statistics for user: {}", clientId);

        UserProfileStatisticsDto statistics = orderService.getUserStatistics(clientId);

        return ResponseEntity.ok(statistics);
    }
}
