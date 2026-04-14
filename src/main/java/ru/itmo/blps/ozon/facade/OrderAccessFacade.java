package ru.itmo.blps.ozon.facade;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.service.OrderService;

@Component
public class OrderAccessFacade {

    private final OrderService orderService;

    public OrderAccessFacade(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OrderResponse createOrder(CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public OrderResponse getOrderById(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PreAuthorize("hasAuthority('ORDER_ACCEPT')")
    public OrderResponse acceptOrder(Long orderId) {
        return orderService.acceptOrder(orderId);
    }

    @PreAuthorize("hasAuthority('ORDER_PACK')")
    public OrderResponse packOrder(Long orderId) {
        return orderService.packOrder(orderId);
    }

    @PreAuthorize("hasAuthority('ORDER_HANDOFF')")
    public OrderResponse handToDelivery(Long orderId, HandToDeliveryRequest request) {
        return orderService.handToDelivery(orderId, request);
    }

    @PreAuthorize("hasAuthority('ORDER_DELIVER')")
    public OrderResponse markDelivered(Long orderId) {
        return orderService.markDelivered(orderId);
    }

    @PreAuthorize("hasAuthority('ORDER_CANCEL')")
    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        return orderService.cancelOrder(orderId, request);
    }
}
