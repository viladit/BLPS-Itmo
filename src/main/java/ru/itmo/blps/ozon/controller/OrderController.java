package ru.itmo.blps.ozon.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.blps.ozon.dto.CancelOrderRequest;
import ru.itmo.blps.ozon.dto.CreateOrderRequest;
import ru.itmo.blps.ozon.dto.HandToDeliveryRequest;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + response.getId())).body(response);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping
    public List<OrderResponse> listOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping("/{orderId}/check-stock")
    public OrderResponse checkStock(@PathVariable Long orderId) {
        return orderService.checkStock(orderId);
    }

    @PostMapping("/{orderId}/reserve")
    public OrderResponse reserveItems(@PathVariable Long orderId) {
        return orderService.reserveItems(orderId);
    }

    @PostMapping("/{orderId}/confirm")
    public OrderResponse confirmOrder(@PathVariable Long orderId) {
        return orderService.confirmOrder(orderId);
    }

    @PostMapping("/{orderId}/picking-task")
    public OrderResponse createPickingTask(@PathVariable Long orderId) {
        return orderService.createPickingTask(orderId);
    }

    @PostMapping("/{orderId}/picked")
    public OrderResponse markPicked(@PathVariable Long orderId) {
        return orderService.markPicked(orderId);
    }

    @PostMapping("/{orderId}/pack")
    public OrderResponse packOrder(@PathVariable Long orderId) {
        return orderService.packOrder(orderId);
    }

    @PostMapping("/{orderId}/handoff")
    public OrderResponse handToDelivery(@PathVariable Long orderId,
                                        @Valid @RequestBody HandToDeliveryRequest request) {
        return orderService.handToDelivery(orderId, request);
    }

    @PostMapping("/{orderId}/deliver")
    public OrderResponse markDelivered(@PathVariable Long orderId) {
        return orderService.markDelivered(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId,
                                     @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancelOrder(orderId, request);
    }
}
