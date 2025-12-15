package sn.dev.order_service.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.controllers.impl.OrderControllerImpl;
import sn.dev.order_service.web.dto.OrderItemRequestDto;
import sn.dev.order_service.web.dto.OrderRequestDto;
import sn.dev.order_service.web.dto.OrderResponseDto;
import sn.dev.order_service.web.mappers.OrdersMappers;
import sn.dev.order_service.web.mappers.SubOrderMapper;

@WebMvcTest(controllers = OrderControllerImpl.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.openfeign.enabled=false"
        },
        excludeAutoConfiguration = {
                org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
        })
@Import(OrderControllerImpl.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrdersMappers ordersMappers;

    @MockitoBean
    private SubOrderMapper subOrderMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testCreateOrder() throws Exception {
        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setUserId("user-1");
        requestDto.setStatus("PENDING");
        requestDto.setPaymentMethod("CREDIT_CARD");
        requestDto.setItems(List.of(new OrderItemRequestDto("prod-1", 2)));

        Order orderEntity = new Order("user-1", 100.0, "PENDING", "CREDIT_CARD");
        orderEntity.setId("order-1");

        when(ordersMappers.toEntity(any(OrderRequestDto.class))).thenReturn(orderEntity);
        when(orderService.create(any(Order.class))).thenReturn(orderEntity);
        
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId("order-1");
        responseDto.setUserId("user-1");
        responseDto.setTotal(100.0);
        responseDto.setStatus("PENDING");
        
        when(ordersMappers.toResponse(any(Order.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/orders")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-1"))
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    @WithMockUser
    void testGetOrderById() throws Exception {
        String orderId = "order-1";
        Order order = new Order("user-1", 100.0, "PENDING", "CREDIT_CARD");
        order.setId(orderId);

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId(orderId);
        responseDto.setUserId("user-1");
        responseDto.setTotal(100.0);

        when(orderService.getById(orderId)).thenReturn(order);
        when(ordersMappers.toResponse(order)).thenReturn(responseDto);

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.total").value(100.0));
    }

    @Test
    @WithMockUser
    void testGetAllOrders() throws Exception {
        Order order1 = new Order("user-1", 100.0, "PENDING", "CREDIT_CARD");
        order1.setId("1");
        Order order2 = new Order("user-2", 200.0, "COMPLETED", "PAYPAL");
        order2.setId("2");

        OrderResponseDto dto1 = new OrderResponseDto();
        dto1.setId("1");
        OrderResponseDto dto2 = new OrderResponseDto();
        dto2.setId("2");

        when(orderService.getAll()).thenReturn(List.of(order1, order2));
        when(ordersMappers.toResponse(order1)).thenReturn(dto1);
        when(ordersMappers.toResponse(order2)).thenReturn(dto2);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }
    
    @Test
    @WithMockUser
    void testGetByUserId() throws Exception {
        String userId = "user-1";
        Order order = new Order(userId, 100.0, "PENDING", "CREDIT_CARD");
        order.setId("order-1");
        
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId("order-1");
        dto.setUserId(userId);

        when(orderService.getByUserId(userId)).thenReturn(List.of(order));
        when(ordersMappers.toResponse(order)).thenReturn(dto);

        mockMvc.perform(get("/api/orders/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId));
    }
}
