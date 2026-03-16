package ru.itmo.blps.ozon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.dto.OrderItemRequest;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.entity.Order;
import ru.itmo.blps.ozon.entity.OrderItem;
import ru.itmo.blps.ozon.entity.OrderStatus;
import ru.itmo.blps.ozon.exception.InvalidOrderStateException;
import ru.itmo.blps.ozon.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-16T10:15:30Z"), ZoneId.of("Europe/Moscow"));
        orderService = new OrderService(orderRepository, fixedClock);
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createOrderShouldMoveOrderToContentsLoaded() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Ivan Petrov");
        request.setDeliveryAddress("Saint Petersburg, Nevsky 1");
        request.setItems(List.of(itemRequest("SKU-1", 2)));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.ORDER_CONTENTS_LOADED);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 16, 13, 15, 30));
    }

    @Test
    void workflowShouldReachDeliveredAndClosedStatus() {
        Order order = orderWithStatus(OrderStatus.ORDER_CONTENTS_LOADED, 2);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.checkStock(1L);
        orderService.reserveItems(1L);
        orderService.confirmOrder(1L);
        orderService.createPickingTask(1L);
        orderService.markPicked(1L);
        orderService.packOrder(1L);

        HandToDeliveryRequest handoffRequest = new HandToDeliveryRequest();
        handoffRequest.setCarrierName("OZON Delivery");
        handoffRequest.setTrackingNumber("TRACK-001");
        orderService.handToDelivery(1L, handoffRequest);

        OrderResponse response = orderService.markDelivered(1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.DELIVERED_AND_CLOSED);
        assertThat(response.getDelivery()).isNotNull();
        assertThat(response.getDelivery().getDeliveredAt()).isEqualTo(LocalDateTime.of(2026, 3, 16, 13, 15, 30));
    }

    @Test
    void reserveItemsShouldFailWhenStockIsUnavailable() {
        Order order = orderWithStatus(OrderStatus.ORDER_CONTENTS_LOADED, 101);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.checkStock(1L);

        assertThatThrownBy(() -> orderService.reserveItems(1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Items cannot be reserved because stock is not available");
    }

    @Test
    void cancelOrderShouldFailAfterHandoffToDelivery() {
        Order order = orderWithStatus(OrderStatus.HANDED_TO_DELIVERY, 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        CancelOrderRequest request = new CancelOrderRequest();
        request.setReason("Client changed the plan");

        assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be cancelled after handoff to delivery");
    }

    private OrderItemRequest itemRequest(String sku, int quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setSku(sku);
        itemRequest.setProductName("Test product");
        itemRequest.setQuantity(quantity);
        itemRequest.setUnitPrice(BigDecimal.valueOf(499.99));
        return itemRequest;
    }

    private Order orderWithStatus(OrderStatus status, int quantity) {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerName("Ivan Petrov");
        order.setDeliveryAddress("Saint Petersburg, Nevsky 1");
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.of(2026, 3, 16, 13, 15, 30));
        order.setUpdatedAt(LocalDateTime.of(2026, 3, 16, 13, 15, 30));

        OrderItem item = new OrderItem();
        item.setId(10L);
        item.setSku("SKU-1");
        item.setProductName("Test product");
        item.setQuantity(quantity);
        item.setUnitPrice(BigDecimal.valueOf(499.99));
        order.addItem(item);

        return order;
    }
}
