package sn.dev.order_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.order_service.client.product.ProductClient;
import sn.dev.order_service.data.entities.Order;
import sn.dev.order_service.data.entities.OrderItem;
import sn.dev.order_service.data.repository.OrderRepository;
import sn.dev.order_service.data.repository.SubOrderRepository;
import sn.dev.order_service.services.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testCreateOrder() {
        Order order = new Order("user-1", 100.0, "PENDING", "CREDIT_CARD");
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order created = orderService.create(order);

        assertNotNull(created);
        assertEquals("user-1", created.getUserId());
        verify(orderRepository).save(order);
    }

    @Test
    void testGetById_Success() {
        String orderId = "order-1";
        Order order = new Order("user-1", 100.0, "PENDING", "CREDIT_CARD");
        order.setId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order found = orderService.getById(orderId);

        assertNotNull(found);
        assertEquals(orderId, found.getId());
    }

    @Test
    void testGetById_NotFound() {
        String orderId = "non-existent";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.getById(orderId));
    }

    @Test
    void testRemoveToCart_Success() {
        String orderId = "order-1";
        String productId = "prod-1";
        
        Order order = new Order();
        order.setId(orderId);
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        items.add(item);
        order.setOrderItemList(items);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.removeToCart(orderId, productId);

        verify(orderRepository).save(order);
        assertEquals(0, order.getOrderItemList().size());
    }

    @Test
    void testRemoveToCart_ProductNotFound() {
        String orderId = "order-1";
        String productId = "prod-1";
        
        Order order = new Order();
        order.setId(orderId);
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProductId("other-prod");
        items.add(item);
        order.setOrderItemList(items);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class, () -> orderService.removeToCart(orderId, productId));
    }
}
