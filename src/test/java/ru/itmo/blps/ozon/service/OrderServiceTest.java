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
    void createOrderShouldCreateAvailableOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Ivan Petrov");
        request.setDeliveryAddress("Saint Petersburg, Nevsky 1");
        request.setItems(List.of(itemRequest("SKU-1", 2)));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getStockAvailable()).isTrue();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 16, 13, 15, 30));
    }

    @Test
    void workflowShouldReachDeliveredStatus() {
        Order order = orderWithStatus(OrderStatus.CREATED, 2);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(1L);
        orderService.packOrder(1L);

        HandToDeliveryRequest handoffRequest = new HandToDeliveryRequest();
        handoffRequest.setCarrierName("OZON Delivery");
        handoffRequest.setTrackingNumber("TRACK-001");
        orderService.handToDelivery(1L, handoffRequest);

        OrderResponse response = orderService.markDelivered(1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(response.getDelivery()).isNotNull();
        assertThat(response.getDelivery().getDeliveredAt()).isEqualTo(LocalDateTime.of(2026, 3, 16, 13, 15, 30));
    }

    @Test
    void createOrderShouldCancelOrderWhenStockIsUnavailable() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Ivan Petrov");
        request.setDeliveryAddress("Saint Petersburg, Nevsky 1");
        request.setItems(List.of(itemRequest("SKU-1", 0)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Заказ отменен: недостаточно товара на складе");
    }

    @Test
    void cancelOrderShouldFailAfterHandoffToDelivery() {
        Order order = orderWithStatus(OrderStatus.IN_DELIVERY, 1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        CancelOrderRequest request = new CancelOrderRequest();
        request.setReason("Client changed the plan");

        assertThatThrownBy(() -> orderService.cancelOrder(1L, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Заказ нельзя отменить после передачи в доставку");
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
        Order order = Order.create(
                "Ivan Petrov",
                "Saint Petersburg, Nevsky 1",
                LocalDateTime.of(2026, 3, 16, 13, 15, 30)
        );
        order.setId(1L);
        order.changeStatus(status);
        order.markStockAvailable(true);

        OrderItem item = OrderItem.create("SKU-1", "Test product", quantity, BigDecimal.valueOf(499.99));
        item.setId(10L);
        order.addItem(item);

        return order;
    }
}
