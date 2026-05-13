package ru.itmo.blps.ozon.facade;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.entity.OrderStatus;
import ru.itmo.blps.ozon.service.OrderService;
import ru.itmo.blps.ozon.service.PendingOrderReminderService;

@Component
public class OrderAccessFacade {

    private final OrderService orderService;
    private final ObjectProvider<PendingOrderReminderService> pendingOrderReminderService;

    public OrderAccessFacade(OrderService orderService,
                             ObjectProvider<PendingOrderReminderService> pendingOrderReminderService) {
        this.orderService = orderService;
        this.pendingOrderReminderService = pendingOrderReminderService;
    }

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OrderResponse createOrder(CreateOrderRequest request) {
        return createOrder(request, false);
    }

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OrderResponse createOrder(CreateOrderRequest request, boolean failNotification) {
        return createOrder(request, failNotification, 0);
    }

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OrderResponse createOrder(CreateOrderRequest request, boolean failNotification, int notificationPauseSeconds) {
        return orderService.createOrder(request, failNotification, notificationPauseSeconds);
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public OrderResponse getOrderById(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderService.getOrdersByStatus(status);
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

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public int processPendingOrderReminders() {
        PendingOrderReminderService service = pendingOrderReminderService.getIfAvailable();
        if (service == null) {
            return 0;
        }
        return service.processPendingOrders();
    }
}
