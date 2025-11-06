package sn.dev.order_service.web.controllers.impl;

import jakarta.validation.Valid;
import java.util.List;
import java.util.logging.Logger;

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
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.controllers.OrderController;
import sn.dev.order_service.web.dto.OrderRequestDto;
import sn.dev.order_service.web.dto.OrderResponseDto;
import sn.dev.order_service.web.mappers.OrdersMappers;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;
    private final OrdersMappers ordersMappers;
    private final String maxAge = "300";
    private static final String USERIDSTR = "userID";
    Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public ResponseEntity<OrderResponseDto> create(
        @Valid OrderRequestDto orderRequestDto
    ) {
        logger.info("CREATE order : " + orderRequestDto);


        Order order = orderService.create(
            ordersMappers.toEntity(orderRequestDto)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(ordersMappers.toResponse(order));
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getAll() {
        logger.info("GET(getAll) orders");

        List<Order> orders = orderService.getAll();
        List<OrderResponseDto> responseList = orders
            .stream()
            .map(ordersMappers::toResponse)
            .toList();

        return ResponseEntity.ok(responseList);
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getByUserId(String userId) {
        logger.info("GET orders by userId: " + userId);

        List<Order> orders = orderService.getByUserId(userId);
        List<OrderResponseDto> responseList = orders
            .stream()
            .map(ordersMappers::toResponse)
            .toList();

        return ResponseEntity.ok(responseList);
    }

    @Override
    public ResponseEntity<OrderResponseDto> getById(String id) {
        logger.info("GET(order by id) order with id: " + id);

        Order order = orderService.getById(id);

        return ResponseEntity.ok(ordersMappers.toResponse(order));
    }

    @Override
    public ResponseEntity<OrderResponseDto> update(
        @Valid OrderRequestDto orderRequestDto,
        String id
    ) {
        logger.info("UPDATE(order by id) order with id: " + id);
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

        Order orderToUpdate = ordersMappers.toEntity(orderRequestDto);
        orderToUpdate.setId(id);

        Order updatedOrder = orderService.update(orderToUpdate);


        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(ordersMappers.toResponse(updatedOrder));
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        logger.info("DELETE(order by id) order with id: " + id);
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
}
